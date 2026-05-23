package org.hectoris919.CBCGoingBallistic.data;

public record PropellantProperties(
		PropellantKind kind,
		double chargeEquivalents,
		Double powderMassOverrideKg,
		Double chargeLengthOverrideMeters,
		double velocityMultiplier,
		double caseMassKg
) {
	public PropellantProperties {
		if (kind == null) kind = PropellantKind.AUTOCANNON_CARTRIDGE;
		if (!Double.isFinite(chargeEquivalents) || chargeEquivalents < 0.0D) chargeEquivalents = 0.0D;
		if (powderMassOverrideKg != null && (!Double.isFinite(powderMassOverrideKg) || powderMassOverrideKg < 0.0D)) powderMassOverrideKg = null;
		if (chargeLengthOverrideMeters != null && (!Double.isFinite(chargeLengthOverrideMeters) || chargeLengthOverrideMeters <= 0.0D)) chargeLengthOverrideMeters = null;
		if (!Double.isFinite(velocityMultiplier) || velocityMultiplier <= 0.0D) velocityMultiplier = 1.0D;
		if (!Double.isFinite(caseMassKg) || caseMassKg < 0.0D) caseMassKg = 0.0D;
	}

	public boolean hasPowderMassOverride() { return this.powderMassOverrideKg != null; }
	public boolean hasChargeLengthOverride() { return this.chargeLengthOverrideMeters != null; }

	public double resolvedPowderMassKg() {
		if (this.powderMassOverrideKg != null) return this.powderMassOverrideKg;
		return switch (this.kind) {
			case BIG_CANNON_CHARGE, BIG_CANNON_CARTRIDGE -> this.chargeEquivalents * BallisticsParameterRegistry.cannonPowderMass();
			case AUTOCANNON_CARTRIDGE, MACHINE_GUN_ROUND -> BallisticsParameterRegistry.autocannonPowderMass();
		};
	}

	public double resolvedChargeLengthMeters() {
		if (this.chargeLengthOverrideMeters != null) return this.chargeLengthOverrideMeters;
		return switch (this.kind) {
			case BIG_CANNON_CHARGE, BIG_CANNON_CARTRIDGE -> Math.max(this.chargeEquivalents, 0.0D) * BallisticsParameterRegistry.cannonChargeLength();
			case AUTOCANNON_CARTRIDGE, MACHINE_GUN_ROUND -> BallisticsParameterRegistry.autocannonCartridgeLength();
		};
	}

	public enum PropellantKind {
		BIG_CANNON_CHARGE,
		BIG_CANNON_CARTRIDGE,
		AUTOCANNON_CARTRIDGE,
		MACHINE_GUN_ROUND;

		public static PropellantKind parse(String raw) {
			if (raw == null || raw.isBlank()) return AUTOCANNON_CARTRIDGE;
			return switch (raw.trim().toLowerCase(java.util.Locale.ROOT)) {
				case "big_cannon_charge", "powder_charge", "charge" -> BIG_CANNON_CHARGE;
				case "big_cannon_cartridge", "big_cartridge" -> BIG_CANNON_CARTRIDGE;
				case "machine_gun_round", "machine_gun" -> MACHINE_GUN_ROUND;
				default -> AUTOCANNON_CARTRIDGE;
			};
		}
	}
}
