package org.hectoris919.CBCGoingBallistic.client;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import org.hectoris919.CBCGoingBallistic.GoingBallistic;
import org.hectoris919.CBCGoingBallistic.data.BallisticsParameterRegistry;
import org.hectoris919.CBCGoingBallistic.data.ProjectileMassProperties;
import org.hectoris919.CBCGoingBallistic.data.ProjectileMassRegistry;
import rbasamoyai.createbigcannons.index.CBCDataComponents;
import rbasamoyai.createbigcannons.munitions.autocannon.AutocannonCartridgeItem;
import rbasamoyai.createbigcannons.munitions.autocannon.AutocannonRoundItem;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class BallisticsTooltipApi {
	private static final double FLUID_SHELL_CAPACITY_MB = 2000.0D;
	private static final ResourceLocation POWDER_CHARGE = ResourceLocation.parse("createbigcannons:powder_charge");
	private static final ResourceLocation BIG_CARTRIDGE = ResourceLocation.parse("createbigcannons:big_cartridge");
	private static final ResourceLocation FILLED_BIG_CARTRIDGE = ResourceLocation.parse("createbigcannons:filled_big_cartridge");
	private static final ResourceLocation AUTOCANNON_CARTRIDGE = ResourceLocation.parse("createbigcannons:autocannon_cartridge");
	private static final ResourceLocation FILLED_AUTOCANNON_CARTRIDGE = ResourceLocation.parse("createbigcannons:filled_autocannon_cartridge");
	private static final ResourceLocation MACHINE_GUN_ROUND = ResourceLocation.parse("createbigcannons:machine_gun_round");

	private static final Map<ResourceLocation, ResourceLocation> ITEM_TO_PROJECTILE = Map.ofEntries(
			Map.entry(ResourceLocation.parse("createbigcannons:solid_shot"), ResourceLocation.parse("createbigcannons:shot")),
			Map.entry(ResourceLocation.parse("createbigcannons:shot"), ResourceLocation.parse("createbigcannons:shot")),
			Map.entry(ResourceLocation.parse("createbigcannons:ap_shot"), ResourceLocation.parse("createbigcannons:ap_shot")),
			Map.entry(ResourceLocation.parse("createbigcannons:ap_shell"), ResourceLocation.parse("createbigcannons:ap_shell")),
			Map.entry(ResourceLocation.parse("createbigcannons:bag_of_grapeshot"), ResourceLocation.parse("createbigcannons:bag_of_grapeshot")),
			Map.entry(ResourceLocation.parse("createbigcannons:grapeshot"), ResourceLocation.parse("createbigcannons:bag_of_grapeshot")),
			Map.entry(ResourceLocation.parse("createbigcannons:he_shell"), ResourceLocation.parse("createbigcannons:he_shell")),
			Map.entry(ResourceLocation.parse("createbigcannons:mortar_stone"), ResourceLocation.parse("createbigcannons:mortar_stone")),
			Map.entry(ResourceLocation.parse("createbigcannons:mortar_stone_projectile"), ResourceLocation.parse("createbigcannons:mortar_stone")),
			Map.entry(ResourceLocation.parse("createbigcannons:drop_mortar_shell"), ResourceLocation.parse("createbigcannons:drop_mortar_shell")),
			Map.entry(ResourceLocation.parse("createbigcannons:shrapnel_shell"), ResourceLocation.parse("createbigcannons:shrapnel_shell")),
			Map.entry(ResourceLocation.parse("createbigcannons:fluid_shell"), ResourceLocation.parse("createbigcannons:fluid_shell")),
			Map.entry(ResourceLocation.parse("createbigcannons:smoke_shell"), ResourceLocation.parse("createbigcannons:smoke_shell")),
			Map.entry(ResourceLocation.parse("createbigcannons:flak_autocannon_round"), ResourceLocation.parse("createbigcannons:flak_autocannon")),
			Map.entry(ResourceLocation.parse("createbigcannons:ap_autocannon_round"), ResourceLocation.parse("createbigcannons:ap_autocannon")),
			Map.entry(MACHINE_GUN_ROUND, ResourceLocation.parse("createbigcannons:machine_gun_bullet"))
	);

	private BallisticsTooltipApi() { }

	public static boolean hasGoingBallisticTooltip(ItemStack stack) {
		if (stack == null || stack.isEmpty()) return false;

		ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
		return getProjectileId(stack, itemId).isPresent() || getPropellantTooltipData(stack, itemId, null) != null;
	}

	public static void appendGoingBallisticTooltip(ItemStack stack, List<Component> tooltip) {
		if (stack == null || stack.isEmpty()) return;

		ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
		boolean headerAdded = false;
		Optional<ResourceLocation> projectileId = getProjectileId(stack, itemId);
		if (projectileId.isPresent()) {
			ProjectileMassProperties properties = ProjectileMassRegistry.getProperties(projectileId.get()).orElse(null);
			if (properties != null) {
				addHeaderIfNeeded(tooltip, headerAdded);
				headerAdded = true;
				appendProjectileMassLine(stack, projectileId.get(), properties, tooltip);
				appendSafeChargeLine(properties, tooltip);
			}
		}

		PropellantTooltipData propellant = getPropellantTooltipData(stack, itemId, projectileId.orElse(null));
		if (propellant != null) {
			addHeaderIfNeeded(tooltip, headerAdded);
			appendPropellantLines(propellant, tooltip);
		}
	}

	private static Optional<ResourceLocation> getProjectileId(ItemStack stack, ResourceLocation itemId) {
		ResourceLocation mapped = ITEM_TO_PROJECTILE.get(itemId);
		if (mapped != null) {
			return Optional.of(mapped);
		}

		if (stack.getItem() instanceof AutocannonRoundItem roundItem) {
			try {
				EntityType<?> entityType = roundItem.getEntityType(stack);
				ResourceLocation entityId = entityType == null
						? null
						: BuiltInRegistries.ENTITY_TYPE.getKey(entityType);
				return entityId == null
						? Optional.empty()
						: Optional.of(entityId);
			} catch (RuntimeException | LinkageError ex) {
				GoingBallistic.LOGGER.debug("Could not read autocannon round projectile id for tooltip from {}", itemId, ex);
			}
		}

		if (stack.getItem() instanceof AutocannonCartridgeItem) {
			ItemStack round = AutocannonCartridgeItem.getProjectileStack(stack);
			if (!round.isEmpty()) {
				return getProjectileId(round, BuiltInRegistries.ITEM.getKey(round.getItem()));
			}
		}

		return Optional.empty();
	}

	private static void appendProjectileMassLine(ItemStack stack, ResourceLocation projectileId, ProjectileMassProperties properties, List<Component> tooltip) {
		double massKg = properties.fluidContainer()
				? getFluidShellMassKg(stack, properties)
				: properties.massKg();
		addIndentedLine(tooltip, Component.translatable("tooltip.cbc_going_ballistic.projectile_mass", formatMass(massKg)), ChatFormatting.GRAY);
	}

	private static void appendSafeChargeLine(ProjectileMassProperties properties, List<Component> tooltip) {
		if (properties.maxSafeChargeEquivalentsOptional().isEmpty()) {
			return;
		}
		double safeCharges = properties.maxSafeChargeEquivalentsOptional().getAsDouble();
		addIndentedLine(tooltip, Component.translatable("tooltip.cbc_going_ballistic.safe_charge_equivalents", formatNumber(safeCharges)), ChatFormatting.GRAY);
	}

	private static PropellantTooltipData getPropellantTooltipData(ItemStack stack, ResourceLocation itemId, ResourceLocation projectileId) {
		if (POWDER_CHARGE.equals(itemId)) {
			double powderMassKg = BallisticsParameterRegistry.cannonPowderMassPerChargeKg();
			return new PropellantTooltipData(powderMassKg, powderMassKg * BallisticsParameterRegistry.blackPowderEnergyJoulesPerKg());
		}

		if (BIG_CARTRIDGE.equals(itemId) || FILLED_BIG_CARTRIDGE.equals(itemId)) {
			double chargeEquivalents = estimateBigCartridgeChargeEquivalents(stack);
			double powderMassKg = chargeEquivalents * BallisticsParameterRegistry.cannonPowderMassPerChargeKg();
			return new PropellantTooltipData(powderMassKg, powderMassKg * BallisticsParameterRegistry.blackPowderEnergyJoulesPerKg());
		}

		if (AUTOCANNON_CARTRIDGE.equals(itemId) || FILLED_AUTOCANNON_CARTRIDGE.equals(itemId)) {
			double powderMassKg = getAutocannonPowderMassForProjectile(projectileId);
			return new PropellantTooltipData(powderMassKg, powderMassKg * BallisticsParameterRegistry.blackPowderEnergyJoulesPerKg());
		}

		if (MACHINE_GUN_ROUND.equals(itemId)) {
			double powderMassKg = getAutocannonPowderMassForProjectile(ResourceLocation.parse("createbigcannons:machine_gun_bullet"));
			return new PropellantTooltipData(powderMassKg, powderMassKg * BallisticsParameterRegistry.blackPowderEnergyJoulesPerKg());
		}

		return null;
	}

	private static double getAutocannonPowderMassForProjectile(ResourceLocation projectileId) {
		ProjectileMassProperties properties = projectileId == null
				? null
				: ProjectileMassRegistry.getProperties(projectileId).orElse(null);
		return properties == null
				? BallisticsParameterRegistry.autocannonPowderMassKg()
				: properties.autocannonPowderMassKgOr(BallisticsParameterRegistry.autocannonPowderMassKg());
	}

	private static double estimateBigCartridgeChargeEquivalents(ItemStack stack) {
		int storedPower = getIntComponent(stack);
		if (storedPower <= 0) return 0.0D;

		return storedPower <= 4
				? storedPower
				: storedPower / 2.0D;
	}

	private static void appendPropellantLines(PropellantTooltipData propellant, List<Component> tooltip) {
		addIndentedLine(tooltip, Component.translatable("tooltip.cbc_going_ballistic.chemical_energy", formatEnergy(propellant.energyJ())), ChatFormatting.GRAY);
		addIndentedLine(tooltip, Component.translatable("tooltip.cbc_going_ballistic.powder_mass", formatMass(propellant.powderMassKg())), ChatFormatting.DARK_GRAY);
	}

	private static void addHeaderIfNeeded(List<Component> tooltip, boolean headerAdded) {
		if (!headerAdded) {
			if (!tooltip.isEmpty()) tooltip.add(Component.empty());
			tooltip.add(Component.translatable("tooltip.cbc_going_ballistic.summary_header").withStyle(ChatFormatting.GOLD));
		}
	}

	private static void addIndentedLine(List<Component> tooltip, Component text, ChatFormatting formatting) {
		tooltip.add(Component.literal(" ").append(text).withStyle(formatting));
	}

	private static double getFluidShellMassKg(ItemStack stack, ProjectileMassProperties properties) {
		FluidPayload payload = getFluidPayload(stack);
		if (payload == null) return properties.massWithFluid(0.0D, properties.referenceFluidDensityKgPerM3());

		double fillFraction = Math.max(0.0D, Math.min(1.0D, payload.amountMb() / FLUID_SHELL_CAPACITY_MB));
		double density = payload.fluid() == null || payload.fluid() == Fluids.EMPTY
				? properties.referenceFluidDensityKgPerM3()
				: payload.fluid().getFluidType().getDensity();

		return properties.massWithFluid(fillFraction, density);
	}

	private static FluidPayload getFluidPayload(ItemStack stack) {
		try {
			CustomData data = getCustomDataComponent(stack);
			if (data.isEmpty()) return new FluidPayload(Fluids.EMPTY, 0);

			CompoundTag tag = data.copyTag();
			int amount = tag.getInt("FluidAmount");
			Fluid fluid = Fluids.EMPTY;
			String rawFluidId = tag.getString("Fluid");
			if (!rawFluidId.isBlank()) {
				try {
					ResourceLocation fluidId = ResourceLocation.parse(rawFluidId);
					fluid = BuiltInRegistries.FLUID.getOptional(fluidId).orElse(Fluids.EMPTY);
				} catch (IllegalArgumentException ignored) { }
			}
			return new FluidPayload(fluid, Math.max(0, amount));
		} catch (RuntimeException | LinkageError ex) {
			GoingBallistic.LOGGER.debug("Could not read fluid shell item payload for tooltip", ex);
			return null;
		}
	}

	private static int getIntComponent(ItemStack stack) {
		return stack.getOrDefault(CBCDataComponents.POWER, 0);
	}

	private static CustomData getCustomDataComponent(ItemStack stack) {
		return stack.getOrDefault(CBCDataComponents.FLUID_CONTENT, CustomData.EMPTY);
	}

	private static String formatMass(double kg) {
		if (!Double.isFinite(kg)) return "? kg";
		if (kg < 1.0D) return String.format(Locale.ROOT, "%.1f g", kg * 1000.0D);
		if (kg < 100.0D) return String.format(Locale.ROOT, "%.3f kg", kg);

		return String.format(Locale.ROOT, "%.1f kg", kg);
	}

	private static String formatEnergy(double joules) {
		if (!Double.isFinite(joules)) return "? J";
		if (joules >= 1.0E9D) return String.format(Locale.ROOT, "%.3f GJ", joules / 1.0E9D);
		if (joules >= 1.0E6D) return String.format(Locale.ROOT, "%.3f MJ", joules / 1.0E6D);
		if (joules >= 1.0E3D) return String.format(Locale.ROOT, "%.3f kJ", joules / 1.0E3D);

		return String.format(Locale.ROOT, "%.1f J", joules);
	}

	private static String formatNumber(double value) {
		return Math.abs(value - Math.rint(value)) < 1.0E-6D
				? String.format(Locale.ROOT, "%.0f", value)
				: String.format(Locale.ROOT, "%.2f", value);
	}

	private record PropellantTooltipData(double powderMassKg, double energyJ) { }

	private record FluidPayload(Fluid fluid, int amountMb) { }
}
