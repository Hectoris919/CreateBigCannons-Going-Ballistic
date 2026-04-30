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

	public static double cannonPowderMassPerChargeKg() {
		return overrides.cannonPowderMassPerChargeKg != null
				? overrides.cannonPowderMassPerChargeKg
				: Config.powderChargeMass();
	}

	public static double cannonChargeLengthPerChargeMeters() {
		return overrides.cannonChargeLengthPerChargeMeters != null
				? overrides.cannonChargeLengthPerChargeMeters
				: Config.powderChargeDiameter();
	}

	public static double autocannonPowderMassKg() {
		return overrides.autocannonPowderMassKg != null
				? overrides.autocannonPowderMassKg
				: Config.autocannonPowderMass();
	}

	public static double autocannonChargeLengthMeters() {
		return overrides.autocannonChargeLengthMeters != null
				? overrides.autocannonChargeLengthMeters
				: Config.autocannonCartridgeDiameter();
	}

	public static double blackPowderEnergyJoulesPerKg() {
		return overrides.blackPowderEnergyJoulesPerKg != null
				? overrides.blackPowderEnergyJoulesPerKg
				: Config.blackPowderEnergyJoulesPerKg();
	}

	public static final class Overrides {
		public static final Overrides EMPTY = new Overrides(null, null, null, null, null, null, null, null, null);

		private final Double robinsConstantMps;
		private final Double velocityMultiplier;
		private final Double maxMuzzleVelocityMps;
		private final Double minBarrelToChargeLengthRatio;
		private final Double cannonPowderMassPerChargeKg;
		private final Double cannonChargeLengthPerChargeMeters;
		private final Double autocannonPowderMassKg;

		private final Double autocannonChargeLengthMeters;
		private final Double blackPowderEnergyJoulesPerKg;

		public Overrides(
				Double robinsConstantMps,
				Double velocityMultiplier,
				Double maxMuzzleVelocityMps,
				Double minBarrelToChargeLengthRatio,
				Double cannonPowderMassPerChargeKg,
				Double cannonChargeLengthPerChargeMeters,
				Double autocannonPowderMassKg,
				Double autocannonChargeLengthMeters,
				Double blackPowderEnergyJoulesPerKg
		) {
			this.robinsConstantMps = robinsConstantMps;
			this.velocityMultiplier = velocityMultiplier;
			this.maxMuzzleVelocityMps = maxMuzzleVelocityMps;
			this.minBarrelToChargeLengthRatio = minBarrelToChargeLengthRatio;
			this.cannonPowderMassPerChargeKg = cannonPowderMassPerChargeKg;
			this.cannonChargeLengthPerChargeMeters = cannonChargeLengthPerChargeMeters;
			this.autocannonPowderMassKg = autocannonPowderMassKg;
			this.autocannonChargeLengthMeters = autocannonChargeLengthMeters;
			this.blackPowderEnergyJoulesPerKg = blackPowderEnergyJoulesPerKg;
		}

		public boolean isEmpty() {
			return this.robinsConstantMps == null
					&& this.velocityMultiplier == null
					&& this.maxMuzzleVelocityMps == null
					&& this.minBarrelToChargeLengthRatio == null
					&& this.cannonPowderMassPerChargeKg == null
					&& this.cannonChargeLengthPerChargeMeters == null
					&& this.autocannonPowderMassKg == null
					&& this.autocannonChargeLengthMeters == null
					&& this.blackPowderEnergyJoulesPerKg == null;
		}
	}

	static final class Builder {
		private Double robinsConstantMps;
		private Double velocityMultiplier;
		private Double maxMuzzleVelocityMps;
		private Double minBarrelToChargeLengthRatio;
		private Double cannonPowderMassPerChargeKg;
		private Double cannonChargeLengthPerChargeMeters;
		private Double autocannonPowderMassKg;
		private Double autocannonChargeLengthMeters;
		private Double blackPowderEnergyJoulesPerKg;

		void robinsConstantMps(double value) { this.robinsConstantMps = value; }
		void velocityMultiplier(double value) { this.velocityMultiplier = value; }
		void maxMuzzleVelocityMps(double value) { this.maxMuzzleVelocityMps = value; }
		void minBarrelToChargeLengthRatio(double value) { this.minBarrelToChargeLengthRatio = value; }
		void cannonPowderMassPerChargeKg(double value) { this.cannonPowderMassPerChargeKg = value; }
		void cannonChargeLengthPerChargeMeters(double value) { this.cannonChargeLengthPerChargeMeters = value; }
		void autocannonPowderMassKg(double value) { this.autocannonPowderMassKg = value; }
		void autocannonChargeLengthMeters(double value) { this.autocannonChargeLengthMeters = value; }
		void blackPowderEnergyJoulesPerKg(double value) { this.blackPowderEnergyJoulesPerKg = value; }

		Overrides build() {
			return new Overrides(
					this.robinsConstantMps,
					this.velocityMultiplier,
					this.maxMuzzleVelocityMps,
					this.minBarrelToChargeLengthRatio,
					this.cannonPowderMassPerChargeKg,
					this.cannonChargeLengthPerChargeMeters,
					this.autocannonPowderMassKg,
					this.autocannonChargeLengthMeters,
					this.blackPowderEnergyJoulesPerKg
			);
		}
	}
}
