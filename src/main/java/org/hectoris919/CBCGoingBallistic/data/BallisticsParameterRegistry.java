package org.hectoris919.CBCGoingBallistic.data;

public final class BallisticsParameterRegistry {
	private static volatile Overrides overrides = Overrides.EMPTY;

	private static final double BLACK_POWDER_DENSITY_KG_PER_M3 = 1700.0D;
	private static final double DEFAULT_CANNON_CHARGE_DIAMETER = (5.0D / 16.0D) + Math.sqrt(Math.pow(5.0D / 16.0D, 2.0D) + Math.pow(5.0D / 16.0D, 2.0D));
	private static final double DEFAULT_AUTOCANNON_CHARGE_DIAMETER = (1.0D / 16.0D) + Math.sqrt(Math.pow(1.0D / 16.0D, 2.0D) + Math.pow(1.0D / 16.0D, 2.0D));
	private static final double DEFAULT_CANNON_POWDER_MASS = BLACK_POWDER_DENSITY_KG_PER_M3 * Math.PI * Math.pow(((2.0D / 16.0D) + Math.sqrt(Math.pow(2.0D / 16.0D, 2.0D) + Math.pow(2.0D / 16.0D, 2.0D))) / 2.0D, 2.0D);
	private static final double DEFAULT_AUTOCANNON_POWDER_MASS = BLACK_POWDER_DENSITY_KG_PER_M3 * Math.PI * 0.75D * Math.pow(DEFAULT_AUTOCANNON_CHARGE_DIAMETER / 2.0D, 2.0D);

	private BallisticsParameterRegistry() { }

	static void replace(Overrides newOverrides) {
		overrides = newOverrides == null
				? Overrides.EMPTY
				: newOverrides;
	}

	public static double robinsConstantMps() { return value(overrides.robinsConstantMps, 606.8568D); }
	public static double velocityMultiplier() { return value(overrides.velocityMultiplier, 1.0D); }
	public static double projectileMassFallback() { return value(overrides.projectileMassFallback, 3519.5D); }
	public static double autocannonProjectileMassFallback() { return value(overrides.autocannonProjectileMassFallback, 33.0D); }
	public static double cannonPowderMass() { return value(overrides.cannonPowderMass, DEFAULT_CANNON_POWDER_MASS); }
	public static double cannonChargeDiameter() { return value(overrides.cannonChargeDiameter, DEFAULT_CANNON_CHARGE_DIAMETER); }
	public static double cannonChargeLength() { return value(overrides.cannonChargeLength, 1.0D); }
	public static double autocannonPowderMass() { return value(overrides.autocannonPowderMass, DEFAULT_AUTOCANNON_POWDER_MASS); }
	public static double autocannonCartridgeDiameter() { return value(overrides.autocannonCartridgeDiameter, DEFAULT_AUTOCANNON_CHARGE_DIAMETER); }
	public static double autocannonCartridgeLength() { return value(overrides.autocannonCartridgeLength, 0.75D); }
	public static double blackPowderEnergyJoulesPerKg() { return value(overrides.blackPowderEnergyJoulesPerKg, 3000000.0D); }
	public static double airDensity() { return value(overrides.airDensity, 1.225D); }
	public static double projectileDragCoefficient() { return value(overrides.projectileDragCoefficient, 0.47D); }
	public static double machineGunBulletRadius() { return value(overrides.machineGunBulletRadius, (0.00782D / 2.0D) * Math.cbrt(0.012D / 0.00945D)); }
	public static double joulesPerToughnessPoint() { return value(overrides.joulesPerToughnessPoint, 8000000.0D); }
	public static double kilogramsPerSableMassUnit() { return value(overrides.kilogramsPerSableMassUnit, 7850.0D / 4.0D); }
	public static double sableRecoilGasVelocityMultiplier() { return value(overrides.sableRecoilGasVelocityMultiplier, 1.2D); }
	public static double sableBlankGasVelocityMps() { return value(overrides.sableBlankGasVelocityMps, 400.0D); }
	public static double sableBlankGasImpulseEfficiency() { return value(overrides.sableBlankGasImpulseEfficiency, 0.35D); }
	public static double sableRecoilImpulseScale() { return value(overrides.sableRecoilImpulseScale, 1.0D); }
	public static double sableImpactImpulseScale() { return value(overrides.sableImpactImpulseScale, 1.0D); }

	private static double value(Double override, double fallback) { return override != null ? override : fallback; }

	public static final class Overrides {
		public static final Overrides EMPTY = new Overrides(
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null
		);

		private final Double robinsConstantMps;
		private final Double velocityMultiplier;
		private final Double projectileMassFallback;
		private final Double autocannonProjectileMassFallback;
		private final Double cannonPowderMass;
		private final Double cannonChargeDiameter;
		private final Double cannonChargeLength;
		private final Double autocannonPowderMass;
		private final Double autocannonCartridgeDiameter;
		private final Double autocannonCartridgeLength;
		private final Double blackPowderEnergyJoulesPerKg;
		private final Double airDensity;
		private final Double projectileDragCoefficient;
		private final Double machineGunBulletRadius;
		private final Double joulesPerToughnessPoint;
		private final Double kilogramsPerSableMassUnit;
		private final Double sableRecoilGasVelocityMultiplier;
		private final Double sableBlankGasVelocityMps;
		private final Double sableBlankGasImpulseEfficiency;
		private final Double sableRecoilImpulseScale;
		private final Double sableImpactImpulseScale;

		public Overrides(
				Double robinsConstantMps,
				Double velocityMultiplier,
				Double projectileMassFallback,
				Double autocannonProjectileMassFallback,
				Double cannonPowderMass,
				Double cannonChargeDiameter,
				Double cannonChargeLength,
				Double autocannonPowderMass,
				Double autocannonCartridgeDiameter,
				Double autocannonCartridgeLength,
				Double blackPowderEnergyJoulesPerKg,
				Double airDensity,
				Double projectileDragCoefficient,
				Double machineGunBulletRadius,
				Double joulesPerToughnessPoint,
				Double kilogramsPerSableMassUnit,
				Double sableRecoilGasVelocityMultiplier,
				Double sableBlankGasVelocityMps,
				Double sableBlankGasImpulseEfficiency,
				Double sableRecoilImpulseScale,
				Double sableImpactImpulseScale
		) {
			this.robinsConstantMps = robinsConstantMps;
			this.velocityMultiplier = velocityMultiplier;
			this.projectileMassFallback = projectileMassFallback;
			this.autocannonProjectileMassFallback = autocannonProjectileMassFallback;
			this.cannonPowderMass = cannonPowderMass;
			this.cannonChargeDiameter = cannonChargeDiameter;
			this.cannonChargeLength = cannonChargeLength;
			this.autocannonPowderMass = autocannonPowderMass;
			this.autocannonCartridgeDiameter = autocannonCartridgeDiameter;
			this.autocannonCartridgeLength = autocannonCartridgeLength;
			this.blackPowderEnergyJoulesPerKg = blackPowderEnergyJoulesPerKg;
			this.airDensity = airDensity;
			this.projectileDragCoefficient = projectileDragCoefficient;
			this.machineGunBulletRadius = machineGunBulletRadius;
			this.joulesPerToughnessPoint = joulesPerToughnessPoint;
			this.kilogramsPerSableMassUnit = kilogramsPerSableMassUnit;
			this.sableRecoilGasVelocityMultiplier = sableRecoilGasVelocityMultiplier;
			this.sableBlankGasVelocityMps = sableBlankGasVelocityMps;
			this.sableBlankGasImpulseEfficiency = sableBlankGasImpulseEfficiency;
			this.sableRecoilImpulseScale = sableRecoilImpulseScale;
			this.sableImpactImpulseScale = sableImpactImpulseScale;
		}

		public boolean isEmpty() {
			return this.robinsConstantMps == null
					&& this.velocityMultiplier == null
					&& this.projectileMassFallback == null
					&& this.autocannonProjectileMassFallback == null
					&& this.cannonPowderMass == null
					&& this.cannonChargeDiameter == null
					&& this.cannonChargeLength == null
					&& this.autocannonPowderMass == null
					&& this.autocannonCartridgeDiameter == null
					&& this.autocannonCartridgeLength == null
					&& this.blackPowderEnergyJoulesPerKg == null
					&& this.airDensity == null
					&& this.projectileDragCoefficient == null
					&& this.machineGunBulletRadius == null
					&& this.joulesPerToughnessPoint == null
					&& this.kilogramsPerSableMassUnit == null
					&& this.sableRecoilGasVelocityMultiplier == null
					&& this.sableBlankGasVelocityMps == null
					&& this.sableBlankGasImpulseEfficiency == null
					&& this.sableRecoilImpulseScale == null
					&& this.sableImpactImpulseScale == null;
		}
	}

	static final class Builder {
		private Double robinsConstantMps;
		private Double velocityMultiplier;
		private Double projectileMassFallback;
		private Double autocannonProjectileMassFallback;
		private Double cannonPowderMass;
		private Double cannonChargeDiameter;
		private Double cannonChargeLength;
		private Double autocannonPowderMass;
		private Double autocannonCartridgeDiameter;
		private Double autocannonCartridgeLength;
		private Double blackPowderEnergyJoulesPerKg;
		private Double airDensity;
		private Double projectileDragCoefficient;
		private Double machineGunBulletRadius;
		private Double joulesPerToughnessPoint;
		private Double kilogramsPerSableMassUnit;
		private Double sableRecoilGasVelocityMultiplier;
		private Double sableBlankGasVelocityMps;
		private Double sableBlankGasImpulseEfficiency;
		private Double sableRecoilImpulseScale;
		private Double sableImpactImpulseScale;

		void robinsConstantMps(double value) { this.robinsConstantMps = value; }
		void velocityMultiplier(double value) { this.velocityMultiplier = value; }
		void projectileMassFallback(double value) { this.projectileMassFallback = value; }
		void autocannonProjectileMassFallback(double value) { this.autocannonProjectileMassFallback = value; }
		void cannonPowderMass(double value) { this.cannonPowderMass = value; }
		void cannonChargeDiameter(double value) { this.cannonChargeDiameter = value; }
		void cannonChargeLength(double value) { this.cannonChargeLength = value; }
		void autocannonPowderMass(double value) { this.autocannonPowderMass = value; }
		void autocannonCartridgeDiameter(double value) { this.autocannonCartridgeDiameter = value; }
		void autocannonCartridgeLength(double value) { this.autocannonCartridgeLength = value; }
		void blackPowderEnergyJoulesPerKg(double value) { this.blackPowderEnergyJoulesPerKg = value; }
		void airDensity(double value) { this.airDensity = value; }
		void projectileDragCoefficient(double value) { this.projectileDragCoefficient = value; }
		void machineGunBulletRadius(double value) { this.machineGunBulletRadius = value; }
		void joulesPerToughnessPoint(double value) { this.joulesPerToughnessPoint = value; }
		void kilogramsPerSableMassUnit(double value) { this.kilogramsPerSableMassUnit = value; }
		void sableRecoilGasVelocityMultiplier(double value) { this.sableRecoilGasVelocityMultiplier = value; }
		void sableBlankGasVelocityMps(double value) { this.sableBlankGasVelocityMps = value; }
		void sableBlankGasImpulseEfficiency(double value) { this.sableBlankGasImpulseEfficiency = value; }
		void sableRecoilImpulseScale(double value) { this.sableRecoilImpulseScale = value; }
		void sableImpactImpulseScale(double value) { this.sableImpactImpulseScale = value; }

		Overrides build() {
			return new Overrides(
					this.robinsConstantMps,
					this.velocityMultiplier,
					this.projectileMassFallback,
					this.autocannonProjectileMassFallback,
					this.cannonPowderMass,
					this.cannonChargeDiameter,
					this.cannonChargeLength,
					this.autocannonPowderMass,
					this.autocannonCartridgeDiameter,
					this.autocannonCartridgeLength,
					this.blackPowderEnergyJoulesPerKg,
					this.airDensity,
					this.projectileDragCoefficient,
					this.machineGunBulletRadius,
					this.joulesPerToughnessPoint,
					this.kilogramsPerSableMassUnit,
					this.sableRecoilGasVelocityMultiplier,
					this.sableBlankGasVelocityMps,
					this.sableBlankGasImpulseEfficiency,
					this.sableRecoilImpulseScale,
					this.sableImpactImpulseScale
			);
		}
	}
}