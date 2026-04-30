package org.hectoris919.CBCGoingBallistic.data;

import java.util.OptionalDouble;

public record ProjectileMassProperties(
		double massKg,
		boolean fluidContainer,
		double emptyMassKg,
		double internalFluidVolumeM3,
		double referenceFluidDensityKgPerM3,
		Double autocannonPowderMassKg,
		Double autocannonChargeLengthMeters,
		Double autocannonVelocityMultiplier,
		Double maxSafeChargeEquivalents
) {
	public ProjectileMassProperties {
		if (!Double.isFinite(massKg) || massKg <= 0.0D) { throw new IllegalArgumentException("Projectile mass must be positive and finite"); }
		if (!Double.isFinite(emptyMassKg) || emptyMassKg < 0.0D) { emptyMassKg = 0.0D; }
		if (!Double.isFinite(internalFluidVolumeM3) || internalFluidVolumeM3 < 0.0D) { internalFluidVolumeM3 = 0.0D; }
		if (!Double.isFinite(referenceFluidDensityKgPerM3) || referenceFluidDensityKgPerM3 < 0.0D) { referenceFluidDensityKgPerM3 = 1000.0D; }
		if (!positiveNullable(autocannonPowderMassKg)) { autocannonPowderMassKg = null; }
		if (!positiveNullable(autocannonChargeLengthMeters)) { autocannonChargeLengthMeters = null; }
		if (!positiveNullable(autocannonVelocityMultiplier)) { autocannonVelocityMultiplier = null; }
		if (!positiveNullable(maxSafeChargeEquivalents)) { maxSafeChargeEquivalents = null; }
	}

	public static ProjectileMassProperties fixed(double massKg, Double autocannonPowderMassKg, Double autocannonChargeLengthMeters, Double autocannonVelocityMultiplier, Double maxSafeChargeEquivalents) {
		return new ProjectileMassProperties(massKg, false, 0.0D, 0.0D, 1000.0D, autocannonPowderMassKg, autocannonChargeLengthMeters, autocannonVelocityMultiplier, maxSafeChargeEquivalents);
	}

	public static ProjectileMassProperties fluidContainer(double emptyMassKg, double internalFluidVolumeM3, double referenceFluidDensityKgPerM3, Double autocannonPowderMassKg, Double autocannonChargeLengthMeters, Double autocannonVelocityMultiplier, Double maxSafeChargeEquivalents) {
		double fullReferenceMassKg = emptyMassKg + internalFluidVolumeM3 * referenceFluidDensityKgPerM3;
		return new ProjectileMassProperties(fullReferenceMassKg, true, emptyMassKg, internalFluidVolumeM3, referenceFluidDensityKgPerM3, autocannonPowderMassKg, autocannonChargeLengthMeters, autocannonVelocityMultiplier, maxSafeChargeEquivalents);
	}

	public double massWithFluid(double fillFraction, double fluidDensityKgPerM3) {
		if (!this.fluidContainer) return this.massKg;
		double safeFill = Math.max(0.0D, Math.min(1.0D, fillFraction));
		double safeDensity = Double.isFinite(fluidDensityKgPerM3) && fluidDensityKgPerM3 > 0.0D
				? fluidDensityKgPerM3
				: this.referenceFluidDensityKgPerM3;
		return this.emptyMassKg + this.internalFluidVolumeM3 * safeFill * safeDensity;
	}

	public double autocannonPowderMassKgOr(double fallback) {
		return this.autocannonPowderMassKg != null
				? this.autocannonPowderMassKg
				: fallback;
	}

	public double autocannonChargeLengthMetersOr(double fallback) {
		return this.autocannonChargeLengthMeters != null
				? this.autocannonChargeLengthMeters
				: fallback;
	}

	public double autocannonVelocityMultiplierOr(double fallback) {
		return this.autocannonVelocityMultiplier != null
				? this.autocannonVelocityMultiplier
				: fallback;
	}

	public OptionalDouble maxSafeChargeEquivalentsOptional() {
		return this.maxSafeChargeEquivalents == null
				? OptionalDouble.empty()
				: OptionalDouble.of(this.maxSafeChargeEquivalents);
	}

	private static boolean positiveNullable(Double value) { return value == null || Double.isFinite(value) && value > 0.0D; }
}
