package org.hectoris919.CBCGoingBallistic.ballistics;

import org.hectoris919.CBCGoingBallistic.Config;
import org.hectoris919.CBCGoingBallistic.GoingBallistic;
import org.hectoris919.CBCGoingBallistic.data.BallisticsParameterRegistry;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public final class BlockDamageMath {
	private static final double MIN_EFFECTIVE_INCIDENCE = 0.2D;
	private static final double BOUNCE_NORMAL_RESTITUTION = 0.7D;
	private static final double CBC_PARTIAL_DAMAGE_POINTS_PER_TOUGHNESS = 10.0D;
	private static final double AUTOCANNON_STRUCTURAL_COUPLING_EXPONENT = 1.0D / 3.0D;

	private BlockDamageMath() { }

	public static double velocityBlocksPerTickToMetersPerSecond(double velocityBlocksPerTick) { return velocityBlocksPerTick * BallisticsMath.TICKS_PER_SECOND; }

	public static double impactEnergyJ(double massKg, double velocityMps) {
		if (!Double.isFinite(massKg) || !Double.isFinite(velocityMps) || massKg <= 0.0D || velocityMps <= 0.0D) return 0.0D;
		return 0.5D * massKg * velocityMps * velocityMps;
	}

	public static double incidence(Vec3 velocity, Vec3 surfaceNormal) {
		if (velocity == null || surfaceNormal == null || velocity.lengthSqr() <= 1.0E-12D || surfaceNormal.lengthSqr() <= 1.0E-12D) {
			return 0.0D;
		}
		return finiteUnitClamp(velocity.normalize().dot(surfaceNormal.reverse()));
	}

	public static double effectiveThicknessMultiplier(double incidence) {
		return 1.0D / Math.max(finiteUnitClamp(incidence), MIN_EFFECTIVE_INCIDENCE);
	}

	public static double perforationWorkJ(double toughness, double incidence) {
		if (!Double.isFinite(toughness) || toughness <= 0.0D) return 0.0D;
		return toughness
				* Math.max(BallisticsParameterRegistry.joulesPerToughnessPoint(), 1.0D)
				* effectiveThicknessMultiplier(incidence);
	}

	public static double perforationScore(double impactEnergyJ, double incidence) {
		if (!Double.isFinite(impactEnergyJ) || impactEnergyJ <= 0.0D) return 0.0D;
		double workPerToughness = Math.max(BallisticsParameterRegistry.joulesPerToughnessPoint(), 1.0D)
				* effectiveThicknessMultiplier(incidence);
		return impactEnergyJ / workPerToughness;
	}

	public static Vec3 velocityAfterEnergyLoss(Vec3 velocityBlocksPerTick, double massKg, double energyLossJ) {
		if (velocityBlocksPerTick == null) return Vec3.ZERO;
		if (!Double.isFinite(massKg) || massKg <= 0.0D
			|| !Double.isFinite(energyLossJ) || energyLossJ <= 0.0D
			|| velocityBlocksPerTick.lengthSqr() <= 1.0E-12D) {
			return velocityBlocksPerTick;
		}

		double velocityMps = velocityBlocksPerTickToMetersPerSecond(velocityBlocksPerTick.length());
		double initialEnergyJ = impactEnergyJ(massKg, velocityMps);
		if (initialEnergyJ <= 0.0D || energyLossJ >= initialEnergyJ) return Vec3.ZERO;

		double remainingEnergyJ = initialEnergyJ - energyLossJ;
		double velocityScale = Math.sqrt(remainingEnergyJ / initialEnergyJ);
		return velocityBlocksPerTick.scale(velocityScale);
	}

	public static double bounceDepositedEnergyJ(double impactEnergyJ, double incidence) {
		if (!Double.isFinite(impactEnergyJ) || impactEnergyJ <= 0.0D) return 0.0D;
		double normalEnergyFraction = Math.pow(finiteUnitClamp(incidence), 2.0D);
		double lostNormalFraction = 1.0D - BOUNCE_NORMAL_RESTITUTION * BOUNCE_NORMAL_RESTITUTION;
		return impactEnergyJ * normalEnergyFraction * lostNormalFraction;
	}

	public static double partialDamagePoints(double depositedEnergyJ, double requiredPerforationWorkJ, double toughness) {
		if (!Double.isFinite(depositedEnergyJ) || depositedEnergyJ <= 0.0D
				|| !Double.isFinite(requiredPerforationWorkJ) || requiredPerforationWorkJ <= 0.0D
				|| !Double.isFinite(toughness) || toughness <= 0.0D) {
			return 0.0D;
		}
		double workFraction = Math.min(depositedEnergyJ / requiredPerforationWorkJ, 1.0D);
		return CBC_PARTIAL_DAMAGE_POINTS_PER_TOUGHNESS * toughness * workFraction;
	}

	public static void logCannonImpact(
			Entity projectile,
			double massKg,
			double velocityMps,
			double incidence,
			double impactEnergyJ,
			double requiredWorkJ,
			double depositedEnergyJ,
			double damageScore,
			double partialDamagePoints,
			double toughness,
			String outcome
	) {
		if (!Config.debugBallistics()) return;
		GoingBallistic.LOGGER.info(
				"[Going Ballistic] Big cannon impact: projectile={}, mass={} kg, velocity={} m/s, incidence={}, impactEnergy={} J, requiredWork={} J, depositedEnergy={} J, perforationScore={}, partialDamage={}, toughness={}, outcome={}",
				BallisticProjectileHelper.getProjectileId(projectile),
				massKg,
				velocityMps,
				incidence,
				impactEnergyJ,
				requiredWorkJ,
				depositedEnergyJ,
				damageScore,
				partialDamagePoints,
				toughness,
				outcome
		);
	}

	public static void logAutocannonImpact(
			Entity projectile,
			double massKg,
			double velocityMps,
			double incidence,
			double impactEnergyJ,
			double requiredWorkJ,
			double depositedEnergyJ,
			double structuralCoupling,
			double structurallyCoupledEnergyJ,
			double damagePoints,
			double hardnessPenalty,
			String outcome
	) {
		if (!Config.debugBallistics()) return;
		GoingBallistic.LOGGER.info(
				"[Going Ballistic] Autocannon impact: projectile={}, mass={} kg, velocity={} m/s, incidence={}, impactEnergy={} J, requiredWork={} J, depositedEnergy={} J, structuralCoupling={}, structurallyCoupledEnergy={} J, blockDamage={}, hardnessPenalty={}, outcome={}",
				BallisticProjectileHelper.getProjectileId(projectile),
				massKg,
				velocityMps,
				incidence,
				impactEnergyJ,
				requiredWorkJ,
				depositedEnergyJ,
				structuralCoupling,
				structurallyCoupledEnergyJ,
				damagePoints,
				hardnessPenalty,
				outcome
		);
	}

	public static double autocannonStructuralDamageCoupling(double massKg) {
		if (!Double.isFinite(massKg) || massKg <= 0.0D) return 0.0D;
		double referenceMassKg = Math.max(BallisticsParameterRegistry.projectileMassFallback(), 1.0E-9D);
		double massRatio = Math.min(massKg / referenceMassKg, 1.0D);
		return Math.pow(massRatio, AUTOCANNON_STRUCTURAL_COUPLING_EXPONENT);
	}
	
	private static double finiteUnitClamp(double value) { return !Double.isFinite(value) ? 0.0D : Math.max(0.0D, Math.min(1.0D, value)); }
}
