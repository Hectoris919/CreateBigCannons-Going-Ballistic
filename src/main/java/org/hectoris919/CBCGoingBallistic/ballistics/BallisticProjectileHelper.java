package org.hectoris919.CBCGoingBallistic.ballistics;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
import net.minecraft.world.level.material.Fluid;
import org.hectoris919.CBCGoingBallistic.Config;
import org.hectoris919.CBCGoingBallistic.GoingBallistic;
import org.hectoris919.CBCGoingBallistic.data.BallisticsParameterRegistry;
import org.hectoris919.CBCGoingBallistic.data.ProjectileMassProperties;
import org.hectoris919.CBCGoingBallistic.data.ProjectileMassRegistry;
import org.hectoris919.CBCGoingBallistic.mixin.FluidShellProjectileAccessor;
import rbasamoyai.createbigcannons.cannon_control.contraption.AbstractMountedCannonContraption;
import rbasamoyai.createbigcannons.munitions.big_cannon.fluid_shell.EndFluidStack;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;

public final class BallisticProjectileHelper {
	private static final double MIN_BARREL_LENGTH_METERS = 0.25D;
	private static final int MAX_BARREL_SCAN_BLOCKS = 512;
	private static final double DEFAULT_FLUID_CONTAINER_CAPACITY_MB = 2000.0D;

	private static final double DRY_BORE_SLIDING_SHEAR_STRESS_PA = 1000000.0D;
	private static final double DRY_BORE_SHOT_START_SHEAR_STRESS_PA = 10000000.0D;
	private static final double SHOT_START_TRAVEL_METERS = 0.02D;

	private BallisticProjectileHelper() { }

	public static float calculateCannonLaunchVelocityBlocksPerTick(Entity projectile, float cbcChargePower, AbstractMountedCannonContraption contraption) {
		if (Config.disableRealisticBallistics()) return cbcChargePower;

		double projectileMassKg = getProjectileMassKg(projectile);
		double barrelLengthMeters = estimateMountedBarrelLengthMeters(contraption);
		double velocityMps = BallisticsMath.getCannonVelocityFromChargePowerMps(projectileMassKg, cbcChargePower, barrelLengthMeters);
		float velocityBlocksPerTick = BallisticsMath.mpsToBPTFloat(velocityMps);

		logCannonCalculation("launch", projectile, projectileMassKg, cbcChargePower, barrelLengthMeters, velocityMps, velocityBlocksPerTick);

		return velocityBlocksPerTick > 0.0F
				? velocityBlocksPerTick
				: cbcChargePower;
	}

	public static float calculateAutocannonLaunchVelocityBlocksPerTick(Entity projectile, float originalVelocity, AbstractMountedCannonContraption contraption) {
		if (Config.disableRealisticBallistics()) return originalVelocity;

		ProjectileMassProperties properties = getProjectileMassProperties(projectile);
		double projectileMassKg = getProjectileMassKg(projectile, Config.autocannonProjectileMassFallback());
		double barrelLengthMeters = estimateMountedBarrelLengthMeters(contraption);
		double powderMassKg = properties == null
				? BallisticsParameterRegistry.autocannonPowderMass()
				: properties.autocannonPowderMassKgOr(BallisticsParameterRegistry.autocannonPowderMass());
		double chargeLengthMeters = properties == null
				? BallisticsParameterRegistry.autocannonCartridgeLength()
				: properties.autocannonChargeLengthMetersOr(BallisticsParameterRegistry.autocannonCartridgeLength());
		double localVelocityMultiplier = properties == null
				? 1.0D
				: properties.autocannonVelocityMultiplierOr(1.0D);
		double velocityMps = BallisticsMath.getRobinsVelocityMps(projectileMassKg, powderMassKg, chargeLengthMeters, barrelLengthMeters, localVelocityMultiplier);
		float velocityBlocksPerTick = BallisticsMath.mpsToBPTFloat(velocityMps);

		logAutocannonCalculation(projectile, projectileMassKg, originalVelocity, powderMassKg, chargeLengthMeters, localVelocityMultiplier, barrelLengthMeters, velocityMps, velocityBlocksPerTick);

		return velocityBlocksPerTick > 0.0F
				? velocityBlocksPerTick
				: originalVelocity;
	}

	public static boolean wouldCannonSquib(Entity projectile, float cbcChargePower, int barrelTravelled) {
		if (Config.disableRealisticBallistics()) return false;
		if (projectile == null) return false;

		double projectileMassKg = getProjectileMassKg(projectile);
		double chargeEquivalent = cbcChargePower / 2.0D;
		double powderMassKg = chargeEquivalent * BallisticsParameterRegistry.cannonPowderMass();
		double chargeLengthMeters = chargeEquivalent * BallisticsParameterRegistry.cannonChargeLength();

		double projectileTravelMeters = Math.max(barrelTravelled, 0.0D);
		double finalGasColumnLengthMeters = chargeLengthMeters + projectileTravelMeters;
		double idealVelocityMps = BallisticsMath.getRobinsVelocityMps(projectileMassKg, powderMassKg, chargeLengthMeters, finalGasColumnLengthMeters);
		double idealProjectileEnergyJ = kineticEnergyJ(projectileMassKg, idealVelocityMps);

		double projectileDiameterMeters = BallisticsParameterRegistry.cannonChargeDiameter();
		double bearingAreaM2 = Math.PI * projectileDiameterMeters;
		double slidingWorkJ = DRY_BORE_SLIDING_SHEAR_STRESS_PA * bearingAreaM2 * projectileTravelMeters;
		double shotStartWorkJ = DRY_BORE_SHOT_START_SHEAR_STRESS_PA * bearingAreaM2 * SHOT_START_TRAVEL_METERS;
		double requiredWorkJ = slidingWorkJ + shotStartWorkJ;

		boolean squib = idealProjectileEnergyJ <= requiredWorkJ;
		logCannonSquibCalculation(projectile, projectileMassKg, cbcChargePower, projectileTravelMeters, finalGasColumnLengthMeters, BallisticsParameterRegistry.cannonChargeDiameter(), idealVelocityMps, idealProjectileEnergyJ, slidingWorkJ, shotStartWorkJ, requiredWorkJ, squib);
		return squib;
	}

	public static ProjectileMassProperties getProjectileMassProperties(Entity projectile) {
		ResourceLocation projectileId = getProjectileId(projectile);
		if (projectileId == null) return null;
		return ProjectileMassRegistry.getProperties(projectileId).orElse(null);
	}

	private static double kineticEnergyJ(double massKg, double velocityMps) {
		if (!Double.isFinite(massKg) || !Double.isFinite(velocityMps)) return 0.0D;
		if (massKg <= 0.0D || velocityMps <= 0.0D) return 0.0D;
		return 0.5D * massKg * velocityMps * velocityMps;
	}

	public static double getProjectileMassKg(Entity projectile) { return getProjectileMassKg(projectile, Config.projectileMassFallback()); }
	public static double getProjectileMassKg(Entity projectile, double fallbackMassKg) {
		if (projectile == null) return fallbackMassKg;

		ResourceLocation projectileId = getProjectileId(projectile);
		ProjectileMassProperties properties = ProjectileMassRegistry.getProperties(projectileId).orElse(null);
		if (properties == null) {
			if (Config.debugBallistics()) GoingBallistic.LOGGER.info("[Going Ballistic] No mass entry for {}; using fallback {} kg", projectileId, fallbackMassKg);
			return fallbackMassKg;
		}

		if (!properties.fluidContainer()) return properties.massKg();

		FluidPayload payload = tryReadFluidPayload(projectile);
		if (payload == null) {
			if (Config.debugBallistics()) GoingBallistic.LOGGER.info("[Going Ballistic] Could not read fluid payload from {}; using reference full-fluid mass {} kg", projectileId, properties.massKg());
			return properties.massKg();
		}

		return properties.massWithFluid(payload.fillFraction(), payload.fluidDensityKgPerM3());
	}

	public static ResourceLocation getProjectileId(Entity projectile) {
		if (projectile == null) return null;
		return BuiltInRegistries.ENTITY_TYPE.getKey(projectile.getType());
	}

	public static double estimateMountedBarrelLengthMeters(AbstractMountedCannonContraption contraption) {
		if (contraption == null) return MIN_BARREL_LENGTH_METERS;

		Direction direction = contraption.initialOrientation();
		BlockPos startPos = contraption.getStartPos();
		Map<BlockPos, StructureBlockInfo> blocks = contraption.getBlocks();
		if (direction == null || startPos == null || blocks == null || blocks.isEmpty()) return MIN_BARREL_LENGTH_METERS;

		int contiguousBlocks = 0;
		BlockPos pos = startPos;
		for (int i = 0; i < MAX_BARREL_SCAN_BLOCKS; ++i) {
			if (!blocks.containsKey(pos)) break;
			++contiguousBlocks;
			pos = pos.relative(direction);
		}

		if (contiguousBlocks <= 0) {
			GoingBallistic.LOGGER.debug("Could not estimate cannon barrel length from mounted contraption {}; using {} m", contraption.getClass().getName(), MIN_BARREL_LENGTH_METERS);
			return MIN_BARREL_LENGTH_METERS;
		}

		return Math.max(contiguousBlocks, MIN_BARREL_LENGTH_METERS);
	}

	private static FluidPayload tryReadFluidPayload(Entity projectile) {
		FluidPayload accessorPayload = tryReadFluidPayloadFromAccessor(projectile);
		if (accessorPayload != null) return accessorPayload;
		return tryReadFluidPayloadReflectively(projectile);
	}

	private static FluidPayload tryReadFluidPayloadFromAccessor(Entity projectile) {
		if (!(projectile instanceof FluidShellProjectileAccessor accessor)) return null;

		try {
			EndFluidStack fluidStack = accessor.goingballistic$getFluidStack();
			if (fluidStack == null || fluidStack.isEmpty()) return new FluidPayload(0.0D, 1000.0D);

			double fillFraction = Math.max(0.0D, Math.min(1.0D, fluidStack.amount() / DEFAULT_FLUID_CONTAINER_CAPACITY_MB));
			double density = fluidStack.fluid() == null
					? 1000.0D
					: fluidStack.fluid().getFluidType().getDensity();
			return new FluidPayload(fillFraction, density);
		} catch (RuntimeException | LinkageError ex) {
			GoingBallistic.LOGGER.debug("Could not read dynamic fluid payload from accessor on projectile {}", projectile.getType(), ex);
			return null;
		}
	}

	private static FluidPayload tryReadFluidPayloadReflectively(Entity projectile) {
		try {
			Field fluidStackField = findField(projectile.getClass(), "fluidStack");
			if (fluidStackField == null) return null;

			fluidStackField.setAccessible(true);
			Object fluidStack = fluidStackField.get(projectile);
			if (fluidStack == null) return new FluidPayload(0.0D, 1000.0D);

			Method amountMethod = fluidStack.getClass().getMethod("amount");
			Object amountValue = amountMethod.invoke(fluidStack);
			double amountMb = amountValue instanceof Number number
					? number.doubleValue()
					: 0.0D;
			double fillFraction = Math.max(0.0D, Math.min(1.0D, amountMb / DEFAULT_FLUID_CONTAINER_CAPACITY_MB));
			double density = 1000.0D;

			try {
				Method fluidMethod = fluidStack.getClass().getMethod("fluid");
				Object fluidValue = fluidMethod.invoke(fluidStack);
				if (fluidValue instanceof Fluid fluid) density = fluid.getFluidType().getDensity();
			} catch (ReflectiveOperationException ignored) {
				GoingBallistic.LOGGER.error("ERROR: ", ignored);
			}

			return new FluidPayload(fillFraction, density);
		} catch (ReflectiveOperationException | LinkageError ex) {
			GoingBallistic.LOGGER.debug("Could not read dynamic fluid payload from projectile {}", projectile.getType(), ex);
			return null;
		}
	}

	private static void logCannonCalculation(String phase, Entity projectile, double projectileMassKg, float cbcChargePower, double barrelLengthMeters, double velocityMps, float velocityBlocksPerTick) {
		if (!Config.debugBallistics()) return;

		double chargeEquivalent = cbcChargePower / 2.0D;
		double powderMassKg = chargeEquivalent * BallisticsParameterRegistry.cannonPowderMass();
		double chargeLengthMeters = chargeEquivalent * BallisticsParameterRegistry.cannonChargeLength();

		GoingBallistic.LOGGER.info(
				"[Going Ballistic] Cannon {}: projectile={}, mass={} kg, chargePower={}, chargeEquivalent={}, powderMass={} kg, chargeLength={} m, barrelLength={} m, velocity={} m/s, velocity={} blocks/tick",
				phase,
				getProjectileId(projectile),
				projectileMassKg,
				cbcChargePower,
				chargeEquivalent,
				powderMassKg,
				chargeLengthMeters,
				barrelLengthMeters,
				velocityMps,
				velocityBlocksPerTick
		);
	}

	private static void logAutocannonCalculation(Entity projectile, double projectileMassKg, float originalVelocity, double powderMassKg, double chargeLengthMeters, double localVelocityMultiplier, double barrelLengthMeters, double velocityMps, float velocityBlocksPerTick) {
		if (!Config.debugBallistics()) return;
		GoingBallistic.LOGGER.info(
				"[Going Ballistic] Autocannon launch: projectile={}, mass={} kg, originalVelocity={} blocks/tick, powderMass={} kg, chargeLength={} m, localVelocityMultiplier={}, barrelLength={} m, velocity={} m/s, velocity={} blocks/tick",
				getProjectileId(projectile),
				projectileMassKg,
				originalVelocity,
				powderMassKg,
				chargeLengthMeters,
				localVelocityMultiplier,
				barrelLengthMeters,
				velocityMps,
				velocityBlocksPerTick
		);
	}

	private static void logCannonSquibCalculation(Entity projectile, double projectileMassKg, float cbcChargePower, double travelledMeters, double finalGasColumnLengthMeters, double projectileDiameterMeters, double idealVelocityMps, double idealProjectileEnergyJ, double slidingWorkJ, double shotStartWorkJ, double requiredWorkJ, boolean squib) {
		if (!Config.debugBallistics()) return;

		double chargeEquivalent = cbcChargePower / 2.0D;
		double powderMassKg = chargeEquivalent * BallisticsParameterRegistry.cannonPowderMass();
		double chargeLengthMeters = chargeEquivalent * BallisticsParameterRegistry.cannonChargeLength();

		GoingBallistic.LOGGER.info(
				"[Going Ballistic] Cannon squib check: projectile={}, mass={} kg, chargePower={}, chargeEquivalent={}, powderMass={} kg, chargeLength={} m, travelled={} m, finalGasColumnLength={} m, projectileDiameter={} m, idealVelocity={} m/s, idealEnergy={} J, slidingWork={} J, shotStartWork={} J, requiredWork={} J, squib={}",
				getProjectileId(projectile),
				projectileMassKg,
				cbcChargePower,
				chargeEquivalent,
				powderMassKg,
				chargeLengthMeters,
				travelledMeters,
				finalGasColumnLengthMeters,
				projectileDiameterMeters,
				idealVelocityMps,
				idealProjectileEnergyJ,
				slidingWorkJ,
				shotStartWorkJ,
				requiredWorkJ,
				squib
		);
	}

	private static Field findField(Class<?> startClass, String name) {
		Class<?> current = startClass;
		while (current != null && current != Object.class) {
			try {
				return current.getDeclaredField(name);
			} catch (NoSuchFieldException ignored) {
				current = current.getSuperclass();
			}
		}
		return null;
	}

	private record FluidPayload(double fillFraction, double fluidDensityKgPerM3) { }
}
