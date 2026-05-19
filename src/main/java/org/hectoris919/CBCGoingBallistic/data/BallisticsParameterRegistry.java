package org.hectoris919.CBCGoingBallistic.data;

import org.hectoris919.CBCGoingBallistic.Config;

public final class BallisticsParameterRegistry {
	private static volatile Overrides overrides = Overrides.EMPTY;

	private BallisticsParameterRegistry() { }

	static void replace(Overrides newOverrides) {
		overrides = newOverrides == null
				? Overrides.EMPTY
				: newOverrides;
	}

	public static double robinsConstantMps() {
		return overrides.robinsConstantMps != null
				? overrides.robinsConstantMps
				: Config.robinsConstantMps();
	}

	public static double velocityMultiplier() {
		return overrides.velocityMultiplier != null
				? overrides.velocityMultiplier
				: Config.velocityMultiplier();
	}

	public static double cannonPowderMass() {
		return overrides.cannonPowderMass != null
				? overrides.cannonPowderMass
				: Config.powderChargeMass();
	}

	public static double cannonChargeDiameter() {
		return overrides.cannonChargeDiameter != null
				? overrides.cannonChargeDiameter
				: Config.powderChargeDiameter();
	}

	public static double cannonChargeLength() {
		return overrides.cannonChargeLength != null
				? overrides.cannonChargeLength
				: Config.powderChargeLength();
	}

	public static double autocannonPowderMass() {
		return overrides.autocannonPowderMass != null
				? overrides.autocannonPowderMass
				: Config.autocannonPowderMass();
	}

	public static double autocannonCartridgeDiameter() {
		return overrides.autocannonCartridgeDiameter != null
				? overrides.autocannonCartridgeDiameter
				: Config.autocannonCartridgeDiameter();
	}

	public static double autocannonCartridgeLength() {
		return overrides.autocannonCartridgeLength != null
				? overrides.autocannonCartridgeLength
				: Config.autocannonCartridgeLength();
	}

	public static double blackPowderEnergyJoulesPerKg() {
		return overrides.blackPowderEnergyJoulesPerKg != null
				? overrides.blackPowderEnergyJoulesPerKg
				: Config.blackPowderEnergyJoulesPerKg();
	}

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
				null
		);

		private final Double robinsConstantMps;
		private final Double velocityMultiplier;
		private final Double cannonPowderMass;
		private final Double cannonChargeDiameter;
		private final Double cannonChargeLength;
		private final Double autocannonPowderMass;
		private final Double autocannonCartridgeDiameter;
		private final Double autocannonCartridgeLength;
		private final Double blackPowderEnergyJoulesPerKg;

		public Overrides(
				Double robinsConstantMps,
				Double velocityMultiplier,
				Double cannonPowderMass,
				Double cannonChargeDiameter,
				Double cannonChargeLength,
				Double autocannonPowderMass,
				Double autocannonCartridgeDiameter,
				Double autocannonCartridgeLength,
				Double blackPowderEnergyJoulesPerKg
		) {
			this.robinsConstantMps = robinsConstantMps;
			this.velocityMultiplier = velocityMultiplier;
			this.cannonPowderMass = cannonPowderMass;
			this.cannonChargeDiameter = cannonChargeDiameter;
			this.cannonChargeLength = cannonChargeLength;
			this.autocannonPowderMass = autocannonPowderMass;
			this.autocannonCartridgeDiameter = autocannonCartridgeDiameter;
			this.autocannonCartridgeLength = autocannonCartridgeLength;
			this.blackPowderEnergyJoulesPerKg = blackPowderEnergyJoulesPerKg;
		}

		public boolean isEmpty() {
			return this.robinsConstantMps == null
					&& this.velocityMultiplier == null
					&& this.cannonPowderMass == null
					&& this.cannonChargeDiameter == null
					&& this.cannonChargeLength == null
					&& this.autocannonPowderMass == null
					&& this.autocannonCartridgeDiameter == null
					&& this.autocannonCartridgeLength == null
					&& this.blackPowderEnergyJoulesPerKg == null;
		}
	}

	static final class Builder {
		private Double robinsConstantMps;
		private Double velocityMultiplier;
		private Double cannonPowderMass;
		private Double cannonChargeDiameter;
		private Double cannonChargeLength;
		private Double autocannonPowderMass;
		private Double autocannonCartridgeDiameter;
		private Double autocannonCartridgeLength;
		private Double blackPowderEnergyJoulesPerKg;

		void robinsConstantMps(double value) { this.robinsConstantMps = value; }
		void velocityMultiplier(double value) { this.velocityMultiplier = value; }
		void cannonPowderMass(double value) { this.cannonPowderMass = value; }
		void cannonChargeDiameter(double value) { this.cannonChargeDiameter = value; }
		void cannonChargeLength(double value) { this.cannonChargeLength = value; }
		void autocannonPowderMass(double value) { this.autocannonPowderMass = value; }
		void autocannonCartridgeDiameter(double value) { this.autocannonCartridgeDiameter = value; }
		void autocannonCartridgeLength(double value) { this.autocannonCartridgeLength = value; }
		void blackPowderEnergyJoulesPerKg(double value) { this.blackPowderEnergyJoulesPerKg = value; }

		Overrides build() {
			return new Overrides(
					this.robinsConstantMps,
					this.velocityMultiplier,
					this.cannonPowderMass,
					this.cannonChargeDiameter,
					this.cannonChargeLength,
					this.autocannonPowderMass,
					this.autocannonCartridgeDiameter,
					this.autocannonCartridgeLength,
					this.blackPowderEnergyJoulesPerKg
			);
		}
	}
}
