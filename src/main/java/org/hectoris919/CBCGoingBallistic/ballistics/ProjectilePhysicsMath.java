package org.hectoris919.CBCGoingBallistic.ballistics;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import org.hectoris919.CBCGoingBallistic.data.BallisticsParameterRegistry;

public final class ProjectilePhysicsMath {
	public static final double TICKS_PER_SECOND = 20.0D;
	public static final double STANDARD_GRAVITY_MPS2 = 9.80665D;

	private ProjectilePhysicsMath() { }

	public static double earthGravityBlocksPerTickSquared() {
		return -STANDARD_GRAVITY_MPS2 / (TICKS_PER_SECOND * TICKS_PER_SECOND);
	}

	public static double cannonShellRadiusMeters() {
		return BallisticsParameterRegistry.cannonChargeDiameter() / 2.0D;
	}

	public static double autocannonShellRadiusMeters() {
		return BallisticsParameterRegistry.autocannonCartridgeDiameter() / 2.0D;
	}

	public static double referenceRadiusMeters(Entity projectile) {
		ResourceLocation projectileId = BallisticProjectileHelper.getProjectileId(projectile);

		if (projectileId == null) return cannonShellRadiusMeters();

		String path = projectileId.getPath();

		if ("machine_gun_bullet".equals(path)) return BallisticsParameterRegistry.machineGunBulletRadius();
		if (path.contains("autocannon")) return autocannonShellRadiusMeters();

		return cannonShellRadiusMeters();
	}

	public static double referenceAreaSquareMeters(double radiusMeters) {
		double safeRadius = safePositive(radiusMeters, cannonShellRadiusMeters());
		return Math.PI * safeRadius * safeRadius;
	}

	public static double quadraticDragCoefficient(double massKg,
												  double radiusMeters,
												  double dragCoefficient,
												  double airDensityKgPerM3) {
		if (!Double.isFinite(massKg) || massKg <= 0.0D) return 0.0D;
		if (!Double.isFinite(dragCoefficient) || dragCoefficient < 0.0D) return 0.0D;
		if (!Double.isFinite(airDensityKgPerM3) || airDensityKgPerM3 < 0.0D) return 0.0D;

		double areaM2 = referenceAreaSquareMeters(radiusMeters);
		return 0.5D * airDensityKgPerM3 * dragCoefficient * areaM2 / massKg;
	}

	public static double dragAccelerationBlocksPerTickSquared(Entity projectile,
															  double velocityBlocksPerTick,
															  double massKg,
															  double dragCoefficient,
															  double airDensityKgPerM3) {

		if (!Double.isFinite(velocityBlocksPerTick) || velocityBlocksPerTick <= 0.0D) return 0.0D;

		double coefficient = quadraticDragCoefficient(
				massKg,
				referenceRadiusMeters(projectile),
				dragCoefficient,
				airDensityKgPerM3
		);

		return coefficient * velocityBlocksPerTick * velocityBlocksPerTick;
	}

	private static double safePositive(double value, double fallback) {
		return Double.isFinite(value) && value > 0.0D
				? value
				: fallback;
	}
}