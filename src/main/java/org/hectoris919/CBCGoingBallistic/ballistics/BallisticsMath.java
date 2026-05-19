package org.hectoris919.CBCGoingBallistic.ballistics;

import org.hectoris919.CBCGoingBallistic.data.BallisticsParameterRegistry;

public final class BallisticsMath {
	public static final double TICKS_PER_SECOND = 20.0D;

	private BallisticsMath() { }

	/**
	 * Robins cannon formula:
	 * v = K * sqrt((p / (m + p / 3)) * ln(L / c))
	 *
	 * @param projectileMassKg projectile mass m, in kg
	 * @param powderMassKg powder mass p, in kg
	 * @param chargeLengthMeters powder charge length c, in meters
	 * @param barrelLengthMeters effective barrel length L, in meters`
	 * @return muzzle velocity in m/s, after config multiplier/cap
	 */
	public static double getRobinsVelocityMps(double projectileMassKg, double powderMassKg, double chargeLengthMeters, double barrelLengthMeters) {
		return getRobinsVelocityMps(projectileMassKg, powderMassKg, chargeLengthMeters, barrelLengthMeters, 1.0D);
	}

	public static double getRobinsVelocityMps(double projectileMassKg, double powderMassKg, double chargeLengthMeters, double barrelLengthMeters, double localVelocityMultiplier) {
		if (!Double.isFinite(localVelocityMultiplier) || localVelocityMultiplier <= 0.0D) localVelocityMultiplier = 1.0D;
		if (!Double.isFinite(projectileMassKg) || !Double.isFinite(powderMassKg) || !Double.isFinite(chargeLengthMeters) || !Double.isFinite(barrelLengthMeters)) return 0.0D;
		if (projectileMassKg <= 0.0D || powderMassKg <= 0.0D || chargeLengthMeters <= 0.0D || barrelLengthMeters <= 0.0D) return 0.0D;

		double ratio = Math.max(barrelLengthMeters / chargeLengthMeters, 1.000001D);
		double logTerm = Math.log(ratio);
		double effectiveMassKg = projectileMassKg + (powderMassKg / 3.0D);
		double radicand = (powderMassKg / effectiveMassKg) * logTerm;

		if (radicand <= 0.0D || !Double.isFinite(radicand)) return 0.0D;

		double velocityMps = BallisticsParameterRegistry.robinsConstantMps() * Math.sqrt(radicand) * localVelocityMultiplier * BallisticsParameterRegistry.velocityMultiplier();
		return Math.max(velocityMps, 0.0D);
	}

	public static double getCannonVelocityMps(double projectileMassKg, double powderCharges, double barrelLengthMeters) {
		if (!Double.isFinite(powderCharges) || powderCharges <= 0.0D) return 0.0D;

		double powderMassKg = powderCharges * BallisticsParameterRegistry.cannonPowderMass();
		double chargeLengthMeters = powderCharges * BallisticsParameterRegistry.cannonChargeLength();

		return getRobinsVelocityMps(projectileMassKg, powderMassKg, chargeLengthMeters, barrelLengthMeters);
	}

	public static double getCannonVelocityFromChargePowerMps(double projectileMassKg, double cbcChargePower, double barrelLengthMeters) { return getCannonVelocityMps(projectileMassKg, cbcChargePower / 2.0D, barrelLengthMeters); }

	// Converts m/s to blocks/tick
	public static double mpsToBPT(double velocityMps) {
		return velocityMps / TICKS_PER_SECOND;
	}

	public static float mpsToBPTFloat(double velocityMps) {
		return (float) mpsToBPT(velocityMps);
	}
}
