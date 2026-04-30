package org.hectoris919.CBCGoingBallistic.ballistics;

import org.hectoris919.CBCGoingBallistic.Config;
import org.hectoris919.CBCGoingBallistic.GoingBallistic;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public final class BlockDamageMath {
	private BlockDamageMath() { }

	public static double velocityBlocksPerTickToMetersPerSecond(double velocityBlocksPerTick) { return velocityBlocksPerTick * BallisticsMath.TICKS_PER_SECOND; }

	public static double impactEnergyJ(double massKg, double velocityMps) {
		if (!Double.isFinite(massKg) || !Double.isFinite(velocityMps) || massKg <= 0.0D || velocityMps <= 0.0D) return 0.0D;
		return 0.5D * massKg * velocityMps * velocityMps;
	}

	public static double normalImpactEnergyJ(double massKg, double velocityMps, double incidence) {
		double safeIncidence = finiteClamp(incidence);
		return impactEnergyJ(massKg, velocityMps) * safeIncidence * safeIncidence;
	}

	public static double cannonDamageScore(double normalImpactEnergyJ, double constructionMultiplier) {
		double joulesPerPoint = Math.max(Config.joulesPerToughnessPoint(), 1.0D);
		return normalImpactEnergyJ / joulesPerPoint * safeMultiplier(constructionMultiplier);
	}

	public static int autocannonDamagePoints(Entity projectile, double normalImpactEnergyJ, double constructionMultiplier) {
		double joulesPerPoint = isMachineGunProjectile(projectile)
				? Math.max(Config.machineGunJoulesPerBlockDamagePoint(), 1.0D)
				: Math.max(Config.autocannonJoulesPerBlockDamagePoint(), 1.0D);
		double rawDamage = normalImpactEnergyJ / joulesPerPoint * safeMultiplier(constructionMultiplier);
		int damage = (int) Math.ceil(Math.max(0.0D, rawDamage));
		int cap = isMachineGunProjectile(projectile)
				? Config.maxMachineGunBlockDamage()
				: Config.maxAutocannonBlockDamage();

		if (cap > 0) damage = Math.min(damage, cap);

		return Math.max(damage, 0);
	}

	public static double hardnessAttenuation(double hardnessPenalty, double projectileToughness) {
		if (hardnessPenalty <= 1.0E-2D) return 1.0D;
		if (projectileToughness <= 1.0E-2D) return 0.0D;

		double min = Math.max(0.0D, Math.min(1.0D, Config.minHardTargetDamageFactor()));
		return Math.max(min, 1.0D - hardnessPenalty / projectileToughness);
	}

	public static double projectileConstructionMultiplier(Entity projectile) {
		String projectileId = String.valueOf(BallisticProjectileHelper.getProjectileId(projectile));
		if (projectileId.contains("ap_autocannon") || projectileId.contains("ap_shot") || projectileId.contains("ap_shell") || projectileId.contains("armor_piercing")) return Config.apProjectileBlockDamageMultiplier();
		if (projectileId.contains("flak") || projectileId.contains("machine_gun")) return Config.smallArmsBlockDamageMultiplier();
		return 1.0D;
	}

	public static boolean isMachineGunProjectile(Entity projectile) {
		String projectileId = String.valueOf(BallisticProjectileHelper.getProjectileId(projectile));
		return projectileId.contains("machine_gun");
	}

	public static double incidence(Vec3 velocity, Vec3 surfaceNormal) {
		if (velocity == null || surfaceNormal == null || velocity.lengthSqr() <= 1.0E-12D || surfaceNormal.lengthSqr() <= 1.0E-12D) {
			return 0.0D;
		}
		return Math.max(0.0D, velocity.normalize().dot(surfaceNormal.reverse()));
	}

	public static void logCannonImpact(Entity projectile, double massKg, double velocityMps, double incidence, double normalEnergyJ, double damageScore, double toughness, boolean blockBroken) {
		if (!Config.debugBallistics()) {
			return;
		}
		GoingBallistic.LOGGER.info(
				"[Going Ballistic] Big cannon impact: projectile={}, mass={} kg, velocity={} m/s, incidence={}, normalEnergy={} J, damageScore={}, toughness={}, blockBroken={}",
				BallisticProjectileHelper.getProjectileId(projectile),
				massKg,
				velocityMps,
				incidence,
				normalEnergyJ,
				damageScore,
				toughness,
				blockBroken
		);
	}

	public static void logAutocannonImpact(Entity projectile, double massKg, double velocityMps, double incidence, double normalEnergyJ, int damagePoints, double hardnessPenalty, double attenuation) {
		if (!Config.debugBallistics()) return;
		GoingBallistic.LOGGER.info(
				"[Going Ballistic] Autocannon impact: projectile={}, mass={} kg, velocity={} m/s, incidence={}, normalEnergy={} J, blockDamage={}, hardnessPenalty={}, hardnessAttenuation={}",
				BallisticProjectileHelper.getProjectileId(projectile),
				massKg,
				velocityMps,
				incidence,
				normalEnergyJ,
				damagePoints,
				hardnessPenalty,
				attenuation
		);
	}

	private static double safeMultiplier(double value) { return Double.isFinite(value) && value > 0.0D ? value : 1.0D; }

	private static double finiteClamp(double value) { return !Double.isFinite(value) ? Math.max(0.0D, Math.min(1.0D, value)) : 0.0D; }
}
