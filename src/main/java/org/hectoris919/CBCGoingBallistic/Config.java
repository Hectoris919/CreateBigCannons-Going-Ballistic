package org.hectoris919.CBCGoingBallistic;

import net.neoforged.neoforge.common.ModConfigSpec;

public class Config {
	private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

	private static final ModConfigSpec.BooleanValue DISABLE_REALISTIC_BALLISTICS = BUILDER
			.comment("Disables Going Ballistic's velocity calculations.")
			.define("disableRealisticBallistics", false);

	private static final ModConfigSpec.BooleanValue DISABLE_REALISTIC_BLOCK_DAMAGE = BUILDER
			.comment("Disables Going Ballistic's impact-energy (kg/m/s) block damage calculation.")
			.define("disableRealisticBlockDamage", false);

	private static final ModConfigSpec.BooleanValue DEBUG_BALLISTICS = BUILDER
			.comment("Logs debug information whenever a shot is calculated.")
			.define("debugBallistics", false);

	private static final ModConfigSpec.DoubleValue VELOCITY_MULTIPLIER = BUILDER
			.comment("Global multiplier applied after the physical muzzle velocity calculation.")
			.defineInRange("velocityMultiplier", 1.0D, 0.01D, 100.0D);

	private static final ModConfigSpec.DoubleValue PROJECTILE_MASS_FALLBACK = BUILDER
			.comment("Mass used when a projectile has no datapack mass entry.")
			.defineInRange("projectileMassFallback", 3519.5D, 0.001D, 1.0E9D);

	private static final ModConfigSpec.DoubleValue AUTOCANNON_PROJECTILE_MASS_FALLBACK = BUILDER
			.comment("Mass used when an autocannon projectile has no datapack mass entry.")
			.defineInRange("autocannonProjectileMassFallback", 33.0D, 0.001D, 1.0E9D);

	private static final ModConfigSpec.DoubleValue ROBINS_CONSTANT_MPS = BUILDER
			.comment("Robins muzzle velocity constant, 1991 ft/s converted to m/s.")
			.defineInRange("robinsConstantMps", 606.8568D, 1.0D, 10000.0D);

	private static final ModConfigSpec.DoubleValue POWDER_CHARGE_MASS = BUILDER
			.comment("Gunpowder mass (kg) within a powder charge.")
			.defineInRange("powderChargeMass", 63.0151276D, 0.001D, 1.0E9D);

	private static final ModConfigSpec.DoubleValue POWDER_CHARGE_DIAMETER = BUILDER
			.comment("Diameter (m) of a powder charge.")
			.defineInRange("powderChargeDiameter", 0.16D, 0.000001D, 10000.0D);

	private static final ModConfigSpec.DoubleValue AUTOCANNON_POWDER_MASS = BUILDER
			.comment("Gunpowder mass (kg) within an autocannon cartridge.")
			.defineInRange("autocannonPowderMass", 11.8153364D, 0.001D, 1.0E9D);

	private static final ModConfigSpec.DoubleValue AUTOCANNON_CARTRIDGE_DIAMETER = BUILDER
			.comment("Diameter (m) of an autocannon cartridge.")
			.defineInRange("autocannonChargeDiameter", 0.75D, 0.000001D, 10000.0D);

	private static final ModConfigSpec.DoubleValue BLACK_POWDER_ENERGY_J_PER_KG = BUILDER
			.comment("Chemical energy within black powder (J/kg)")
			.defineInRange("blackPowderEnergyJoulesPerKg", 3000000.0D, 1.0D, 1.0E12D);

	private static final ModConfigSpec.DoubleValue JOULES_PER_TOUGHNESS_POINT = BUILDER
			.comment("Impact energy required per block toughness point for a projectile to penetrate a block.")
			.defineInRange("blockDamage.joulesPerToughnessPoint", 2000000.0D, 1.0D, 1.0E12D);

	private static final ModConfigSpec.DoubleValue AUTOCANNON_JOULES_PER_BLOCK_DAMAGE_POINT = BUILDER
			.comment("Impact energy required for autocannon rounds to impart a partial block damage point on a block.")
			.defineInRange("blockDamage.autocannonJoulesPerBlockDamagePoint", 25000.0D, 1.0D, 1.0E12D);

	private static final ModConfigSpec.DoubleValue MACHINE_GUN_JOULES_PER_BLOCK_DAMAGE_POINT = BUILDER
			.comment("Impact energy required for machine gun bullets to impart a partial block damage point on a block.")
			.defineInRange("blockDamage.machineGunJoulesPerBlockDamagePoint", 12000.0D, 1.0D, 1.0E12D);

	private static final ModConfigSpec.IntValue MAX_AUTOCANNON_BLOCK_DAMAGE = BUILDER
			.comment("Maximum partial block damage points an autocannon hit may apply. Set to 0 to disable.")
			.defineInRange("blockDamage.maxAutocannonBlockDamage", 0, 0, 1000000);

	private static final ModConfigSpec.IntValue MAX_MACHINE_GUN_BLOCK_DAMAGE = BUILDER
			.comment("Maximum partial block damage points a machine-gun bullet may apply. Set to 0 to disable.")
			.defineInRange("blockDamage.maxMachineGunBlockDamage", 0, 0, 1000000);

	private static final ModConfigSpec.DoubleValue MIN_HARD_TARGET_DAMAGE_FACTOR = BUILDER
			.comment("Minimum fraction of autocannon/machine gun block damage retained after block hardness vs projectile toughness attenuation.")
			.defineInRange("blockDamage.minHardTargetDamageFactor", 0.05D, 0.0D, 1.0D);

	private static final ModConfigSpec.DoubleValue AP_PROJECTILE_BLOCK_DAMAGE_MULTIPLIER = BUILDER
			.comment("Block damage multiplier for AP-named projectiles.")
			.defineInRange("blockDamage.apProjectileBlockDamageMultiplier", 1.75D, 0.0D, 1000.0D);

	private static final ModConfigSpec.DoubleValue SMALL_ARMS_BLOCK_DAMAGE_MULTIPLIER = BUILDER
			.comment("Block damage multiplier for flak and machine gun projectiles.")
			.defineInRange("blockDamage.smallArmsBlockDamageMultiplier", 0.35D, 0.0D, 1000.0D);

	public static final ModConfigSpec SPEC = BUILDER.build();

	public static boolean disableRealisticBallistics() { return DISABLE_REALISTIC_BALLISTICS.get(); }
	public static boolean disableRealisticBlockDamage() { return DISABLE_REALISTIC_BLOCK_DAMAGE.get(); }
	public static boolean debugBallistics() { return DEBUG_BALLISTICS.get(); }
	public static double velocityMultiplier() { return VELOCITY_MULTIPLIER.get(); }
	public static double projectileMassFallback() { return PROJECTILE_MASS_FALLBACK.get(); }
	public static double autocannonProjectileMassFallback() { return AUTOCANNON_PROJECTILE_MASS_FALLBACK.get(); }
	public static double robinsConstantMps() { return ROBINS_CONSTANT_MPS.get(); }
	public static double powderChargeMass() { return POWDER_CHARGE_MASS.get(); }
	public static double powderChargeDiameter() { return POWDER_CHARGE_DIAMETER.get(); }
	public static double autocannonPowderMass() { return AUTOCANNON_POWDER_MASS.get(); }
	public static double autocannonCartridgeDiameter() { return AUTOCANNON_CARTRIDGE_DIAMETER.get(); }
	public static double blackPowderEnergyJoulesPerKg() { return BLACK_POWDER_ENERGY_J_PER_KG.get(); }
	public static double joulesPerToughnessPoint() { return JOULES_PER_TOUGHNESS_POINT.get(); }
	public static double autocannonJoulesPerBlockDamagePoint() { return AUTOCANNON_JOULES_PER_BLOCK_DAMAGE_POINT.get(); }
	public static double machineGunJoulesPerBlockDamagePoint() { return MACHINE_GUN_JOULES_PER_BLOCK_DAMAGE_POINT.get(); }
	public static int maxAutocannonBlockDamage() { return MAX_AUTOCANNON_BLOCK_DAMAGE.get(); }
	public static int maxMachineGunBlockDamage() { return MAX_MACHINE_GUN_BLOCK_DAMAGE.get(); }
	public static double minHardTargetDamageFactor() { return MIN_HARD_TARGET_DAMAGE_FACTOR.get(); }
	public static double apProjectileBlockDamageMultiplier() { return AP_PROJECTILE_BLOCK_DAMAGE_MULTIPLIER.get(); }
	public static double smallArmsBlockDamageMultiplier() { return SMALL_ARMS_BLOCK_DAMAGE_MULTIPLIER.get(); }
}
