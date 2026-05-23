package org.hectoris919.CBCGoingBallistic.api;

public record GoingBallisticShotData(
		double projectileMassKg,
		Double powderMassKg,
		Double chargeLengthMeters,
		double velocityMultiplier
) {
	public GoingBallisticShotData {
		if (!Double.isFinite(projectileMassKg) || projectileMassKg <= 0.0D) projectileMassKg = 0.0D;
		if (powderMassKg != null && (!Double.isFinite(powderMassKg) || powderMassKg < 0.0D)) powderMassKg = null;
		if (chargeLengthMeters != null && (!Double.isFinite(chargeLengthMeters) || chargeLengthMeters <= 0.0D)) chargeLengthMeters = null;
		if (!Double.isFinite(velocityMultiplier) || velocityMultiplier <= 0.0D) velocityMultiplier = 1.0D;
	}

	public boolean hasProjectileMass() { return this.projectileMassKg > 0.0D; }
	public double projectileMassKgOr(double fallback) { return this.hasProjectileMass() ? this.projectileMassKg : fallback; }
	public double powderMassKgOr(double fallback) { return this.powderMassKg != null ? this.powderMassKg : fallback; }
	public double chargeLengthMetersOr(double fallback) { return this.chargeLengthMeters != null ? this.chargeLengthMeters : fallback; }
}
