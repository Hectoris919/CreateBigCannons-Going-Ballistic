package org.hectoris919.CBCGoingBallistic.data;

import java.util.OptionalDouble;

public record ProjectileMassProperties(
		double massKg,
		boolean fluidContainer,
		double emptyMassKg,
		double internalFluidVolumeM3,
		double referenceFluidDensityKgPerM3,
		boolean projectileContainer,
		int maxContainedProjectiles,
		boolean allowRecursiveContainers,
		String childItemListPath,
		double childFallbackMassKg,
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
		if (maxContainedProjectiles < 0) { maxContainedProjectiles = 0; }
		if (childItemListPath == null || childItemListPath.isBlank()) { childItemListPath = "Shells"; }
		if (!Double.isFinite(childFallbackMassKg) || childFallbackMassKg < 0.0D) { childFallbackMassKg = BallisticsParameterRegistry.projectileMassFallback(); }
		if (!positiveNullable(autocannonPowderMassKg)) { autocannonPowderMassKg = null; }
		if (!positiveNullable(autocannonChargeLengthMeters)) { autocannonChargeLengthMeters = null; }
		if (!positiveNullable(autocannonVelocityMultiplier)) { autocannonVelocityMultiplier = null; }
		if (!positiveNullable(maxSafeChargeEquivalents)) { maxSafeChargeEquivalents = null; }
	}

	public static ProjectileMassProperties fixed(double massKg, Double autocannonPowderMassKg, Double autocannonChargeLengthMeters, Double autocannonVelocityMultiplier, Double maxSafeChargeEquivalents) {
		return new ProjectileMassProperties(massKg, false, 0.0D, 0.0D, 1000.0D, false, 0, false, "Shells", BallisticsParameterRegistry.projectileMassFallback(), autocannonPowderMassKg, autocannonChargeLengthMeters, autocannonVelocityMultiplier, maxSafeChargeEquivalents);
	}

	public static ProjectileMassProperties fluidContainer(double emptyMassKg, double internalFluidVolumeM3, double referenceFluidDensityKgPerM3, Double autocannonPowderMassKg, Double autocannonChargeLengthMeters, Double autocannonVelocityMultiplier, Double maxSafeChargeEquivalents) {
		double fullReferenceMassKg = Math.max(0.001D, emptyMassKg + internalFluidVolumeM3 * referenceFluidDensityKgPerM3);
		return new ProjectileMassProperties(fullReferenceMassKg, true, emptyMassKg, internalFluidVolumeM3, referenceFluidDensityKgPerM3, false, 0, false, "Shells", BallisticsParameterRegistry.projectileMassFallback(), autocannonPowderMassKg, autocannonChargeLengthMeters, autocannonVelocityMultiplier, maxSafeChargeEquivalents);
	}

	public static ProjectileMassProperties projectileContainer(double emptyMassKg, int maxContainedProjectiles, boolean allowRecursiveContainers, String childItemListPath, double childFallbackMassKg, Double maxSafeChargeEquivalents) {
		return new ProjectileMassProperties(Math.max(0.001D, emptyMassKg), false, emptyMassKg, 0.0D, 1000.0D, true, maxContainedProjectiles, allowRecursiveContainers, childItemListPath, childFallbackMassKg, null, null, null, maxSafeChargeEquivalents);
	}

	public ProjectileMassProperties withMassModifiers(double multiplier, double offsetKg) {
		double safeMultiplier = Double.isFinite(multiplier) && multiplier > 0.0D ? multiplier : 1.0D;
		double safeOffset = Double.isFinite(offsetKg) ? offsetKg : 0.0D;
		if (this.fluidContainer) {
			return new ProjectileMassProperties(Math.max(0.001D, this.massKg * safeMultiplier + safeOffset), true, Math.max(0.0D, this.emptyMassKg * safeMultiplier + safeOffset), this.internalFluidVolumeM3 * safeMultiplier, this.referenceFluidDensityKgPerM3, false, 0, false, this.childItemListPath, this.childFallbackMassKg, this.autocannonPowderMassKg, this.autocannonChargeLengthMeters, this.autocannonVelocityMultiplier, this.maxSafeChargeEquivalents);
		}
		if (this.projectileContainer) {
			return new ProjectileMassProperties(Math.max(0.001D, this.massKg * safeMultiplier + safeOffset), false, Math.max(0.0D, this.emptyMassKg * safeMultiplier + safeOffset), 0.0D, 1000.0D, true, this.maxContainedProjectiles, this.allowRecursiveContainers, this.childItemListPath, this.childFallbackMassKg, this.autocannonPowderMassKg, this.autocannonChargeLengthMeters, this.autocannonVelocityMultiplier, this.maxSafeChargeEquivalents);
		}
		return new ProjectileMassProperties(Math.max(0.001D, this.massKg * safeMultiplier + safeOffset), false, 0.0D, 0.0D, 1000.0D, false, 0, false, this.childItemListPath, this.childFallbackMassKg, this.autocannonPowderMassKg, this.autocannonChargeLengthMeters, this.autocannonVelocityMultiplier, this.maxSafeChargeEquivalents);
	}

	public ProjectileMassProperties withLaunchOverrides(Double autocannonPowderMassKg, Double autocannonChargeLengthMeters, Double autocannonVelocityMultiplier, Double maxSafeChargeEquivalents) {
		return new ProjectileMassProperties(this.massKg, this.fluidContainer, this.emptyMassKg, this.internalFluidVolumeM3, this.referenceFluidDensityKgPerM3, this.projectileContainer, this.maxContainedProjectiles, this.allowRecursiveContainers, this.childItemListPath, this.childFallbackMassKg,
				autocannonPowderMassKg != null ? autocannonPowderMassKg : this.autocannonPowderMassKg,
				autocannonChargeLengthMeters != null ? autocannonChargeLengthMeters : this.autocannonChargeLengthMeters,
				autocannonVelocityMultiplier != null ? autocannonVelocityMultiplier : this.autocannonVelocityMultiplier,
				maxSafeChargeEquivalents != null ? maxSafeChargeEquivalents : this.maxSafeChargeEquivalents);
	}

	public double massWithFluid(double fillFraction, double fluidDensityKgPerM3) {
		if (!this.fluidContainer) return this.massKg;
		double safeFill = Math.max(0.0D, Math.min(1.0D, fillFraction));
		double safeDensity = Double.isFinite(fluidDensityKgPerM3) && fluidDensityKgPerM3 > 0.0D
				? fluidDensityKgPerM3
				: this.referenceFluidDensityKgPerM3;
		return Math.max(0.001D, this.emptyMassKg + this.internalFluidVolumeM3 * safeFill * safeDensity);
	}

	public double massWithChildren(double childMassKg) {
		if (!this.projectileContainer) return this.massKg;
		double safeChildMass = Double.isFinite(childMassKg) && childMassKg > 0.0D ? childMassKg : 0.0D;
		return Math.max(0.001D, this.emptyMassKg + safeChildMass);
	}

	public double autocannonPowderMassKgOr(double fallback) { return this.autocannonPowderMassKg != null ? this.autocannonPowderMassKg : fallback; }
	public double autocannonChargeLengthMetersOr(double fallback) { return this.autocannonChargeLengthMeters != null ? this.autocannonChargeLengthMeters : fallback; }
	public double autocannonVelocityMultiplierOr(double fallback) { return this.autocannonVelocityMultiplier != null ? this.autocannonVelocityMultiplier : fallback; }

	public OptionalDouble maxSafeChargeEquivalentsOptional() {
		return this.maxSafeChargeEquivalents == null ? OptionalDouble.empty() : OptionalDouble.of(this.maxSafeChargeEquivalents);
	}

	private static boolean positiveNullable(Double value) { return value == null || Double.isFinite(value) && value > 0.0D; }
}
