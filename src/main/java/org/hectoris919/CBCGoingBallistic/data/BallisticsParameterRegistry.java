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
	public static double joulesPerToughnessPoint() { return value(overrides.joulesPerToughnessPoint, 2000000.0D); }
	public static double autocannonJoulesPerBlockDamagePoint() { return value(overrides.autocannonJoulesPerBlockDamagePoint, 25000.0D); }
	public static double machineGunJoulesPerBlockDamagePoint() { return value(overrides.machineGunJoulesPerBlockDamagePoint, 12000.0D); }
	public static double minHardTargetDamageFactor() { return value(overrides.minHardTargetDamageFactor, 0.05D); }
	public static double apProjectileBlockDamageMultiplier() { return value(overrides.apProjectileBlockDamageMultiplier, 1.75D); }
	public static double smallArmsBlockDamageMultiplier() { return value(overrides.smallArmsBlockDamageMultiplier, 0.35D); }

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
		private final Double autocannonJoulesPerBlockDamagePoint;
		private final Double machineGunJoulesPerBlockDamagePoint;
		private final Double minHardTargetDamageFactor;
		private final Double apProjectileBlockDamageMultiplier;
		private final Double smallArmsBlockDamageMultiplier;

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
				Double autocannonJoulesPerBlockDamagePoint,
				Double machineGunJoulesPerBlockDamagePoint,
				Double minHardTargetDamageFactor,
				Double apProjectileBlockDamageMultiplier,
				Double smallArmsBlockDamageMultiplier
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
			this.autocannonJoulesPerBlockDamagePoint = autocannonJoulesPerBlockDamagePoint;
			this.machineGunJoulesPerBlockDamagePoint = machineGunJoulesPerBlockDamagePoint;
			this.minHardTargetDamageFactor = minHardTargetDamageFactor;
			this.apProjectileBlockDamageMultiplier = apProjectileBlockDamageMultiplier;
			this.smallArmsBlockDamageMultiplier = smallArmsBlockDamageMultiplier;
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
					&& this.autocannonJoulesPerBlockDamagePoint == null
					&& this.machineGunJoulesPerBlockDamagePoint == null
					&& this.minHardTargetDamageFactor == null
					&& this.apProjectileBlockDamageMultiplier == null
					&& this.smallArmsBlockDamageMultiplier == null;
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
		private Double autocannonJoulesPerBlockDamagePoint;
		private Double machineGunJoulesPerBlockDamagePoint;
		private Double minHardTargetDamageFactor;
		private Double apProjectileBlockDamageMultiplier;
		private Double smallArmsBlockDamageMultiplier;

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
		void autocannonJoulesPerBlockDamagePoint(double value) { this.autocannonJoulesPerBlockDamagePoint = value; }
		void machineGunJoulesPerBlockDamagePoint(double value) { this.machineGunJoulesPerBlockDamagePoint = value; }
		void minHardTargetDamageFactor(double value) { this.minHardTargetDamageFactor = value; }
		void apProjectileBlockDamageMultiplier(double value) { this.apProjectileBlockDamageMultiplier = value; }
		void smallArmsBlockDamageMultiplier(double value) { this.smallArmsBlockDamageMultiplier = value; }

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
					this.autocannonJoulesPerBlockDamagePoint,
					this.machineGunJoulesPerBlockDamagePoint,
					this.minHardTargetDamageFactor,
					this.apProjectileBlockDamageMultiplier,
					this.smallArmsBlockDamageMultiplier
			);
		}
	}
}
