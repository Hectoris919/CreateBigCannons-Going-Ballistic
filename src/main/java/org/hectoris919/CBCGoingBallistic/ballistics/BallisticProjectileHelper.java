package org.hectoris919.CBCGoingBallistic.ballistics;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import org.hectoris919.CBCGoingBallistic.Config;
import org.hectoris919.CBCGoingBallistic.api.GoingBallisticShotData;
import org.hectoris919.CBCGoingBallistic.api.GoingBallisticShotDataHolder;
import org.hectoris919.CBCGoingBallistic.GoingBallistic;
import org.hectoris919.CBCGoingBallistic.data.BallisticsParameterRegistry;
import org.hectoris919.CBCGoingBallistic.data.CannonComponentProperties;
import org.hectoris919.CBCGoingBallistic.data.CannonComponentRegistry;
import org.hectoris919.CBCGoingBallistic.data.ItemProjectileRegistry;
import org.hectoris919.CBCGoingBallistic.data.ProjectileMassProperties;
import org.hectoris919.CBCGoingBallistic.data.ProjectileMassRegistry;
import org.hectoris919.CBCGoingBallistic.data.PropellantProperties;
import org.hectoris919.CBCGoingBallistic.data.PropellantRegistry;
import org.hectoris919.CBCGoingBallistic.mixin.FluidShellProjectileAccessor;
import rbasamoyai.createbigcannons.cannon_control.contraption.AbstractMountedCannonContraption;
import rbasamoyai.createbigcannons.index.CBCDataComponents;
import rbasamoyai.createbigcannons.munitions.big_cannon.fluid_shell.EndFluidStack;
import rbasamoyai.createbigcannons.munitions.autocannon.AutocannonCartridgeItem;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

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
		BarrelProfile barrelProfile = estimateMountedBarrelProfile(contraption);
		double barrelLengthMeters = barrelProfile.lengthMeters();
		double velocityMps = BallisticsMath.getCannonVelocityFromChargePowerMps(projectileMassKg, cbcChargePower, barrelLengthMeters) * barrelProfile.velocityMultiplier();
		float velocityBlocksPerTick = BallisticsMath.mpsToBPTFloat(velocityMps);

		logCannonCalculation("launch", projectile, projectileMassKg, cbcChargePower, barrelLengthMeters, velocityMps, velocityBlocksPerTick);

		return velocityBlocksPerTick > 0.0F
				? velocityBlocksPerTick
				: cbcChargePower;
	}

	public static float calculateAutocannonLaunchVelocityBlocksPerTick(Entity projectile, float originalVelocity, AbstractMountedCannonContraption contraption) {
		if (Config.disableRealisticBallistics()) return originalVelocity;

		ProjectileMassProperties properties = getProjectileMassProperties(projectile);
		GoingBallisticShotData shotData = getShotData(projectile);
		double projectileMassKg = shotData == null
				? getProjectileMassKg(projectile, BallisticsParameterRegistry.autocannonProjectileMassFallback())
				: shotData.projectileMassKgOr(getProjectileMassKg(projectile, BallisticsParameterRegistry.autocannonProjectileMassFallback()));
		BarrelProfile barrelProfile = estimateMountedBarrelProfile(contraption);
		double barrelLengthMeters = barrelProfile.lengthMeters();
		double powderMassKg = properties == null
				? BallisticsParameterRegistry.autocannonPowderMass()
				: properties.autocannonPowderMassKgOr(BallisticsParameterRegistry.autocannonPowderMass());
		double chargeLengthMeters = properties == null
				? BallisticsParameterRegistry.autocannonCartridgeLength()
				: properties.autocannonChargeLengthMetersOr(BallisticsParameterRegistry.autocannonCartridgeLength());
		double localVelocityMultiplier = properties == null
				? 1.0D
				: properties.autocannonVelocityMultiplierOr(1.0D);
		if (shotData != null) {
			powderMassKg = shotData.powderMassKgOr(powderMassKg);
			chargeLengthMeters = shotData.chargeLengthMetersOr(chargeLengthMeters);
			localVelocityMultiplier *= shotData.velocityMultiplier();
		}
		double velocityMps = BallisticsMath.getRobinsVelocityMps(projectileMassKg, powderMassKg, chargeLengthMeters, barrelLengthMeters, localVelocityMultiplier) * barrelProfile.velocityMultiplier();
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

	public static GoingBallisticShotData getShotData(Entity projectile) {
		return projectile instanceof GoingBallisticShotDataHolder holder ? holder.goingballistic$getShotData() : null;
	}

	public static void attachShotData(Entity projectile, GoingBallisticShotData data) {
		if (projectile instanceof GoingBallisticShotDataHolder holder) holder.goingballistic$setShotData(data);
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

	public static double getProjectileMassKg(Entity projectile) { return getProjectileMassKg(projectile, BallisticsParameterRegistry.projectileMassFallback()); }
	public static double getProjectileMassKg(Entity projectile, double fallbackMassKg) {
		if (projectile == null) return fallbackMassKg;

		GoingBallisticShotData shotData = getShotData(projectile);
		if (shotData != null && shotData.hasProjectileMass()) return shotData.projectileMassKg();

		ResourceLocation projectileId = getProjectileId(projectile);
		ProjectileMassProperties properties = ProjectileMassRegistry.getProperties(projectileId).orElse(null);
		if (properties == null) {
			if (Config.debugBallistics()) GoingBallistic.LOGGER.info("[Going Ballistic] No mass entry for {}; using fallback {} kg", projectileId, fallbackMassKg);
			return fallbackMassKg;
		}

		if (properties.projectileContainer()) {
			double childMass = tryReadContainedProjectileMass(projectile, properties);
			if (childMass >= 0.0D) return properties.massWithChildren(childMass);
			return properties.massKg();
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

	public static BarrelProfile estimateMountedBarrelProfile(AbstractMountedCannonContraption contraption) {
		if (contraption == null) return new BarrelProfile(MIN_BARREL_LENGTH_METERS, 1.0D);

		Direction direction = contraption.initialOrientation();
		BlockPos startPos = contraption.getStartPos();
		Map<BlockPos, StructureBlockInfo> blocks = contraption.getBlocks();
		if (direction == null || startPos == null || blocks == null || blocks.isEmpty()) return new BarrelProfile(MIN_BARREL_LENGTH_METERS, 1.0D);

		double lengthMeters = 0.0D;
		double velocityMultiplier = 1.0D;
		BlockPos pos = startPos;
		for (int i = 0; i < MAX_BARREL_SCAN_BLOCKS; ++i) {
			StructureBlockInfo blockInfo = blocks.get(pos);
			if (blockInfo == null) break;

			ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(blockInfo.state().getBlock());
			CannonComponentProperties component = CannonComponentRegistry.get(blockId).orElse(CannonComponentProperties.normalBarrel());
			lengthMeters += component.lengthMeters();
			velocityMultiplier *= component.velocityMultiplier();
			pos = pos.relative(direction);
		}

		if (lengthMeters <= 0.0D) {
			GoingBallistic.LOGGER.debug("Could not estimate cannon barrel length from mounted contraption {}; using {} m", contraption.getClass().getName(), MIN_BARREL_LENGTH_METERS);
			return new BarrelProfile(MIN_BARREL_LENGTH_METERS, 1.0D);
		}

		return new BarrelProfile(Math.max(lengthMeters, MIN_BARREL_LENGTH_METERS), velocityMultiplier);
	}

	private static double tryReadContainedProjectileMass(Entity projectile, ProjectileMassProperties properties) {
		if (projectile == null) return -1.0D;
		try {
			CompoundTag tag = projectile.saveWithoutId(new CompoundTag());
			ListTag childList = getCompoundListAtPath(tag, properties.childItemListPath());
			if (childList == null || childList.isEmpty()) return 0.0D;

			HolderLookup.Provider registries = projectile.level().registryAccess();
			double childMass = 0.0D;
			int maxChildren = properties.maxContainedProjectiles() > 0 ? properties.maxContainedProjectiles() : childList.size();
			for (int i = 0; i < childList.size() && i < maxChildren; ++i) {
				CompoundTag childTag = childList.getCompound(i);
				ItemStack childStack = parseItemStack(registries, childTag);
				if (childStack.isEmpty()) {
					childMass += properties.childFallbackMassKg();
					continue;
				}
				childMass += getProjectileItemMassKg(childStack, registries, properties.childFallbackMassKg(), properties.allowRecursiveContainers(), new HashSet<>());
			}
			return childMass;
		} catch (RuntimeException | LinkageError ex) {
			GoingBallistic.LOGGER.debug("Could not read contained projectile mass from {}", projectile.getType(), ex);
			return -1.0D;
		}
	}

	public static double getProjectileItemMassKg(ItemStack stack, HolderLookup.Provider registries, double fallbackMassKg, boolean allowRecursiveContainers, Set<ResourceLocation> visitedProjectiles) {
		if (stack == null || stack.isEmpty()) return 0.0D;
		ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
		Optional<ResourceLocation> projectileId = ItemProjectileRegistry.getProjectileId(stack, registries).or(() -> ItemProjectileRegistry.getProjectileId(itemId));
		if (projectileId.isEmpty()) return fallbackMassKg;
		ProjectileMassProperties properties = ProjectileMassRegistry.getProperties(projectileId.get()).orElse(null);
		if (properties == null) return fallbackMassKg;

		if (properties.projectileContainer()) {
			if (!allowRecursiveContainers && !visitedProjectiles.add(projectileId.get())) return properties.massKg();
			double children = tryReadContainedProjectileMass(stack, registries, properties, allowRecursiveContainers, visitedProjectiles);
			return properties.massWithChildren(children);
		}

		if (properties.fluidContainer()) {
			FluidPayload payload = tryReadFluidPayloadFromItemStack(stack, registries);
			return payload == null ? properties.massKg() : properties.massWithFluid(payload.fillFraction(), payload.fluidDensityKgPerM3());
		}

		return properties.massKg();
	}

	public static void attachShotDataFromAutocannonAmmoStack(Entity projectile, ItemStack firedStack, HolderLookup.Provider registries) {
		if (projectile == null || firedStack == null || firedStack.isEmpty()) return;
		GoingBallisticShotData data = buildShotDataFromAutocannonAmmoStack(firedStack, registries);
		if (data != null) attachShotData(projectile, data);
	}

	public static GoingBallisticShotData buildShotDataFromAutocannonAmmoStack(ItemStack firedStack, HolderLookup.Provider registries) {
		if (firedStack == null || firedStack.isEmpty()) return null;
		ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(firedStack.getItem());
		Optional<ResourceLocation> projectileId = ItemProjectileRegistry.getProjectileId(firedStack, registries)
				.or(() -> ItemProjectileRegistry.getProjectileId(itemId));

		ItemStack projectileStack = ItemStack.EMPTY;
		if (firedStack.getItem() instanceof AutocannonCartridgeItem) {
			try {
				projectileStack = AutocannonCartridgeItem.getProjectileStack(firedStack);
				if (projectileId.isEmpty() && !projectileStack.isEmpty()) {
					ResourceLocation projectileItemId = BuiltInRegistries.ITEM.getKey(projectileStack.getItem());
					projectileId = ItemProjectileRegistry.getProjectileId(projectileStack, registries)
							.or(() -> ItemProjectileRegistry.getProjectileId(projectileItemId));
				}
			} catch (RuntimeException | LinkageError ex) {
				GoingBallistic.LOGGER.debug("Could not read autocannon cartridge projectile stack from {}", itemId, ex);
			}
		}

		ProjectileMassProperties projectileProperties = projectileId.flatMap(ProjectileMassRegistry::getProperties).orElse(null);
		double projectileMassKg = 0.0D;
		if (projectileProperties != null) {
			ItemStack stackForMass = projectileStack.isEmpty() ? firedStack : projectileStack;
			projectileMassKg = projectileProperties.fluidContainer() || projectileProperties.projectileContainer()
					? getProjectileItemMassKg(stackForMass, registries, projectileProperties.massKg(), false, new HashSet<>())
					: projectileProperties.massKg();
		}

		PropellantProperties propellant = PropellantRegistry.get(firedStack, registries)
				.or(() -> PropellantRegistry.get(itemId))
				.orElse(null);
		Double powderMassKg = null;
		Double chargeLengthMeters = null;
		double velocityMultiplier = 1.0D;

		if (propellant != null) {
			if (propellant.kind() == PropellantProperties.PropellantKind.AUTOCANNON_CARTRIDGE || propellant.kind() == PropellantProperties.PropellantKind.MACHINE_GUN_ROUND) {
				powderMassKg = propellant.hasPowderMassOverride()
						? propellant.resolvedPowderMassKg()
						: projectileProperties == null ? propellant.resolvedPowderMassKg() : projectileProperties.autocannonPowderMassKgOr(propellant.resolvedPowderMassKg());
				chargeLengthMeters = propellant.hasChargeLengthOverride()
						? propellant.resolvedChargeLengthMeters()
						: projectileProperties == null ? propellant.resolvedChargeLengthMeters() : projectileProperties.autocannonChargeLengthMetersOr(propellant.resolvedChargeLengthMeters());
				velocityMultiplier *= propellant.velocityMultiplier();
			}
		}

		if (projectileMassKg <= 0.0D && powderMassKg == null) return null;
		return new GoingBallisticShotData(projectileMassKg, powderMassKg, chargeLengthMeters, velocityMultiplier);
	}

	private static double tryReadContainedProjectileMass(ItemStack stack, HolderLookup.Provider registries, ProjectileMassProperties properties, boolean allowRecursiveContainers, Set<ResourceLocation> visitedProjectiles) {
		try {
			CustomData customData = stack.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA, CustomData.EMPTY);
			CompoundTag tag = customData.copyTag();
			ListTag childList = getCompoundListAtPath(tag, properties.childItemListPath());
			if (childList == null || childList.isEmpty()) return 0.0D;
			double childMass = 0.0D;
			int maxChildren = properties.maxContainedProjectiles() > 0 ? properties.maxContainedProjectiles() : childList.size();
			for (int i = 0; i < childList.size() && i < maxChildren; ++i) {
				ItemStack childStack = parseItemStack(registries, childList.getCompound(i));
				childMass += childStack.isEmpty()
						? properties.childFallbackMassKg()
						: getProjectileItemMassKg(childStack, registries, properties.childFallbackMassKg(), allowRecursiveContainers, visitedProjectiles);
			}
			return childMass;
		} catch (RuntimeException | LinkageError ex) {
			GoingBallistic.LOGGER.debug("Could not read contained projectile mass from item stack {}", stack, ex);
			return 0.0D;
		}
	}

	private static ListTag getCompoundListAtPath(CompoundTag root, String path) {
		if (root == null || path == null || path.isBlank()) return null;
		CompoundTag current = root;
		String[] parts = path.split("\\.");
		for (int i = 0; i < parts.length; ++i) {
			String part = parts[i];
			if (part.isBlank()) continue;
			if (i == parts.length - 1) {
				return current.contains(part, Tag.TAG_LIST) ? current.getList(part, Tag.TAG_COMPOUND) : null;
			}
			if (!current.contains(part, Tag.TAG_COMPOUND)) return null;
			current = current.getCompound(part);
		}
		return null;
	}

	private static ItemStack parseItemStack(HolderLookup.Provider registries, CompoundTag tag) {
		if (tag == null || tag.isEmpty() || registries == null) return ItemStack.EMPTY;
		ItemStack direct = ItemStack.parseOptional(registries, tag);
		if (!direct.isEmpty()) return direct;
		for (String nestedKey : new String[] { "Item", "item", "Stack", "stack" }) {
			if (tag.contains(nestedKey, Tag.TAG_COMPOUND)) {
				ItemStack nested = ItemStack.parseOptional(registries, tag.getCompound(nestedKey));
				if (!nested.isEmpty()) return nested;
			}
		}
		return ItemStack.EMPTY;
	}

	private static FluidPayload tryReadFluidPayloadFromItemStack(ItemStack stack, HolderLookup.Provider registries) {
		try {
			CustomData data = stack.getOrDefault(CBCDataComponents.FLUID_CONTENT, CustomData.EMPTY);
			if (data.isEmpty() || registries == null) return new FluidPayload(0.0D, 1000.0D);
			FluidTank tank = new FluidTank((int) DEFAULT_FLUID_CONTAINER_CAPACITY_MB);
			tank.readFromNBT(registries, data.copyTag());
			FluidStack fluidStack = tank.getFluid();
			if (fluidStack.isEmpty()) return new FluidPayload(0.0D, 1000.0D);
			double fillFraction = Math.max(0.0D, Math.min(1.0D, fluidStack.getAmount() / DEFAULT_FLUID_CONTAINER_CAPACITY_MB));
			double density = fluidStack.getFluid() == Fluids.EMPTY ? 1000.0D : fluidStack.getFluid().getFluidType().getDensity();
			return new FluidPayload(fillFraction, density);
		} catch (RuntimeException | LinkageError ex) {
			GoingBallistic.LOGGER.debug("Could not read dynamic fluid payload from item stack {}", stack, ex);
			return null;
		}
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

	public record BarrelProfile(double lengthMeters, double velocityMultiplier) {
		public BarrelProfile {
			if (!Double.isFinite(lengthMeters) || lengthMeters <= 0.0D) lengthMeters = MIN_BARREL_LENGTH_METERS;
			if (!Double.isFinite(velocityMultiplier) || velocityMultiplier <= 0.0D) velocityMultiplier = 1.0D;
		}
	}

	private record FluidPayload(double fillFraction, double fluidDensityKgPerM3) { }
}
