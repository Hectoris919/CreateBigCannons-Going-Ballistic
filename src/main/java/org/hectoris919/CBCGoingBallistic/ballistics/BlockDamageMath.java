package org.hectoris919.CBCGoingBallistic.ballistics;

import org.hectoris919.CBCGoingBallistic.Config;
import org.hectoris919.CBCGoingBallistic.GoingBallistic;
import org.hectoris919.CBCGoingBallistic.data.BallisticsParameterRegistry;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import rbasamoyai.createbigcannons.munitions.AbstractCannonProjectile.ImpactResult.KinematicOutcome;

public final class BlockDamageMath {
	private static final double MIN_EFFECTIVE_INCIDENCE = 0.2D;
	private static final double BOUNCE_NORMAL_RESTITUTION = 0.7D;
	private static final double CBC_PARTIAL_DAMAGE_POINTS_PER_TOUGHNESS = 10.0D;

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

	public static ImpactSolution solveImpact(
			Vec3 velocityBlocksPerTick,
			Vec3 surfaceNormal,
			double massKg,
			double blockToughness,
			double blockHardness,
			double projectilePenetration,
			double projectileToughness,
			double projectileDeflection,
			float initialDurabilityMass,
			boolean surfaceImpact,
			boolean canBounce,
			boolean unbreakable,
			boolean allowPenetration,
			double baseBounceChance,
			double bounceRoll
	) {
		Vec3 velocity = velocityBlocksPerTick == null ? Vec3.ZERO : velocityBlocksPerTick;
		Vec3 normal = surfaceNormal == null ? Vec3.ZERO : surfaceNormal;

		double incidence = incidence(velocity, normal);
		double velocityMps = velocityBlocksPerTickToMetersPerSecond(velocity.length());
		double impactEnergyJ = impactEnergyJ(massKg, velocityMps);
		double requiredWorkJ = perforationWorkJ(blockToughness, incidence);
		double damageScore = perforationScore(impactEnergyJ, incidence);

		double hardnessPenalty = blockHardness - projectilePenetration;
		double bounceBonus = Math.max(1.0D - hardnessPenalty, 0.0D);
		double bounceChance = projectileDeflection < 1.0E-2D || incidence > projectileDeflection
				? 0.0D
				: Math.max(baseBounceChance, 1.0D - incidence / projectileDeflection) * bounceBonus;

		boolean hasPerforationEnergy = !unbreakable && impactEnergyJ >= requiredWorkJ;
		KinematicOutcome outcome;
		if (surfaceImpact && canBounce && bounceRoll < bounceChance) {
			outcome = KinematicOutcome.BOUNCE;
		} else if (hasPerforationEnergy && allowPenetration) {
			outcome = KinematicOutcome.PENETRATE;
		} else {
			outcome = KinematicOutcome.STOP;
		}

		boolean blockBroken = hasPerforationEnergy && outcome != KinematicOutcome.BOUNCE;
		boolean shatter = surfaceImpact
				&& outcome != KinematicOutcome.BOUNCE
				&& hardnessPenalty > projectileToughness;

		float durabilityPenalty = damageScore < 1.0E-4D
				? initialDurabilityMass
				: (float) (((Math.max(0.0D, hardnessPenalty) + 1.0D) * blockToughness) / damageScore);

		double depositedEnergyJ;
		if (outcome == KinematicOutcome.BOUNCE) {
			depositedEnergyJ = bounceDepositedEnergyJ(impactEnergyJ, incidence);
		} else if (blockBroken) {
			depositedEnergyJ = requiredWorkJ;
		} else {
			depositedEnergyJ = impactEnergyJ;
		}

		double partialDamagePoints = blockBroken || unbreakable
				? 0.0D
				: partialDamagePoints(depositedEnergyJ, requiredWorkJ, blockToughness);

		Vec3 postImpactVelocity = switch (outcome) {
			case PENETRATE -> velocityAfterEnergyLoss(velocity, massKg, requiredWorkJ);
			case BOUNCE -> SableImpulseMath.bouncedVelocity(velocity, normal);
			case STOP -> Vec3.ZERO;
		};

		return new ImpactSolution(
				outcome,
				blockBroken,
				shatter,
				incidence,
				velocityMps,
				impactEnergyJ,
				requiredWorkJ,
				damageScore,
				hardnessPenalty,
				bounceChance,
				depositedEnergyJ,
				partialDamagePoints,
				durabilityPenalty,
				postImpactVelocity
		);
	}

	public static void logImpact(
			String projectileFamily,
			Entity projectile,
			double massKg,
			ImpactSolution impact
	) {
		if (!Config.debugBallistics()) return;
		GoingBallistic.LOGGER.info(
				"[Going Ballistic] {} impact: projectile={}, mass={} kg, velocity={} m/s, incidence={}, impactEnergy={} J, requiredWork={} J, depositedEnergy={} J, perforationScore={}, partialDamage={}, hardnessPenalty={}, bounceChance={}, outcome={}",
				projectileFamily,
				BallisticProjectileHelper.getProjectileId(projectile),
				massKg,
				impact.velocityMps(),
				impact.incidence(),
				impact.impactEnergyJ(),
				impact.requiredWorkJ(),
				impact.depositedEnergyJ(),
				impact.damageScore(),
				impact.partialDamagePoints(),
				impact.hardnessPenalty(),
				impact.bounceChance(),
				impact.outcome().name()
		);
	}

	public record ImpactSolution(
			KinematicOutcome outcome,
			boolean blockBroken,
			boolean shatter,
			double incidence,
			double velocityMps,
			double impactEnergyJ,
			double requiredWorkJ,
			double damageScore,
			double hardnessPenalty,
			double bounceChance,
			double depositedEnergyJ,
			double partialDamagePoints,
			float durabilityPenalty,
			Vec3 postImpactVelocity
	) { }
	
	private static double finiteUnitClamp(double value) { return !Double.isFinite(value) ? 0.0D : Math.max(0.0D, Math.min(1.0D, value)); }
}
