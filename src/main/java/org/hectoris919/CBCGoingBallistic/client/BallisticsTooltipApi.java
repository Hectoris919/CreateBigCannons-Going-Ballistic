package org.hectoris919.CBCGoingBallistic.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import org.hectoris919.CBCGoingBallistic.GoingBallistic;
import org.hectoris919.CBCGoingBallistic.ballistics.BallisticProjectileHelper;
import org.hectoris919.CBCGoingBallistic.data.BallisticsParameterRegistry;
import org.hectoris919.CBCGoingBallistic.data.ItemProjectileRegistry;
import org.hectoris919.CBCGoingBallistic.data.ProjectileMassProperties;
import org.hectoris919.CBCGoingBallistic.data.ProjectileMassRegistry;
import org.hectoris919.CBCGoingBallistic.data.PropellantProperties;
import org.hectoris919.CBCGoingBallistic.data.PropellantRegistry;
import rbasamoyai.createbigcannons.index.CBCDataComponents;
import rbasamoyai.createbigcannons.munitions.autocannon.AutocannonCartridgeItem;
import rbasamoyai.createbigcannons.munitions.autocannon.AutocannonRoundItem;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public final class BallisticsTooltipApi {
	private static final ResourceLocation BIG_CARTRIDGE = ResourceLocation.parse("createbigcannons:big_cartridge");
	private static final ResourceLocation FILLED_BIG_CARTRIDGE = ResourceLocation.parse("createbigcannons:filled_big_cartridge");
	private static final ResourceLocation AUTOCANNON_CARTRIDGE = ResourceLocation.parse("createbigcannons:autocannon_cartridge");
	private static final ResourceLocation FILLED_AUTOCANNON_CARTRIDGE = ResourceLocation.parse("createbigcannons:filled_autocannon_cartridge");
	private static final ResourceLocation MACHINE_GUN_BULLET = ResourceLocation.parse("createbigcannons:machine_gun_bullet");

	private BallisticsTooltipApi() { }

	private static HolderLookup.Provider registryAccess() {
		return Minecraft.getInstance().level == null ? null : Minecraft.getInstance().level.registryAccess();
	}

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
				appendProjectileMassLine(stack, properties, tooltip);
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
		Optional<ResourceLocation> mapped = ItemProjectileRegistry.getProjectileId(stack, registryAccess());
		if (mapped.isPresent()) return mapped;

		mapped = ItemProjectileRegistry.getProjectileId(itemId);
		if (mapped.isPresent()) return mapped;

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
			if (!round.isEmpty()) return getProjectileId(round, BuiltInRegistries.ITEM.getKey(round.getItem()));
		}

		return Optional.empty();
	}

	private static void appendProjectileMassLine(ItemStack stack, ProjectileMassProperties properties, List<Component> tooltip) {
		double massKg;
		if (properties.fluidContainer() || properties.projectileContainer()) {
			HolderLookup.Provider registries = Minecraft.getInstance().level == null
					? null
					: Minecraft.getInstance().level.registryAccess();
			massKg = BallisticProjectileHelper.getProjectileItemMassKg(stack, registries, properties.massKg(), false, new HashSet<>());
			if (!Double.isFinite(massKg) || massKg <= 0.0D) massKg = properties.massKg();
		} else {
			massKg = properties.massKg();
		}
		addIndentedLine(tooltip, Component.translatable("tooltip.cbc_going_ballistic.projectile_mass", formatMass(massKg)), ChatFormatting.GRAY);
	}

	private static void appendSafeChargeLine(ProjectileMassProperties properties, List<Component> tooltip) {
		if (properties.maxSafeChargeEquivalentsOptional().isEmpty()) return;
		double safeCharges = properties.maxSafeChargeEquivalentsOptional().getAsDouble();
		addIndentedLine(tooltip, Component.translatable("tooltip.cbc_going_ballistic.safe_charge_equivalents", formatNumber(safeCharges)), ChatFormatting.GRAY);
	}

	private static PropellantTooltipData getPropellantTooltipData(ItemStack stack, ResourceLocation itemId, ResourceLocation projectileId) {
		PropellantProperties propellant = PropellantRegistry.get(stack, registryAccess()).orElseGet(() -> PropellantRegistry.get(itemId).orElse(null));
		if (propellant != null) {
			double powderMassKg = resolvePowderMass(stack, itemId, projectileId, propellant);
			return new PropellantTooltipData(powderMassKg, powderMassKg * BallisticsParameterRegistry.blackPowderEnergyJoulesPerKg());
		}

		if (BIG_CARTRIDGE.equals(itemId) || FILLED_BIG_CARTRIDGE.equals(itemId)) {
			double chargeEquivalents = estimateBigCartridgeChargeEquivalents(stack);
			double powderMassKg = chargeEquivalents * BallisticsParameterRegistry.cannonPowderMass();
			return new PropellantTooltipData(powderMassKg, powderMassKg * BallisticsParameterRegistry.blackPowderEnergyJoulesPerKg());
		}

		if (AUTOCANNON_CARTRIDGE.equals(itemId) || FILLED_AUTOCANNON_CARTRIDGE.equals(itemId)) {
			double powderMassKg = getAutocannonPowderMassForProjectile(projectileId);
			return new PropellantTooltipData(powderMassKg, powderMassKg * BallisticsParameterRegistry.blackPowderEnergyJoulesPerKg());
		}

		return null;
	}

	private static double resolvePowderMass(ItemStack stack, ResourceLocation itemId, ResourceLocation projectileId, PropellantProperties propellant) {
		if ((BIG_CARTRIDGE.equals(itemId) || FILLED_BIG_CARTRIDGE.equals(itemId)) && stack.has(CBCDataComponents.POWER)) {
			return estimateBigCartridgeChargeEquivalents(stack) * BallisticsParameterRegistry.cannonPowderMass();
		}
		if (propellant.kind() == PropellantProperties.PropellantKind.AUTOCANNON_CARTRIDGE && projectileId != null) {
			ProjectileMassProperties projectileProperties = ProjectileMassRegistry.getProperties(projectileId).orElse(null);
			if (projectileProperties != null && !propellant.hasPowderMassOverride()) {
				return projectileProperties.autocannonPowderMassKgOr(BallisticsParameterRegistry.autocannonPowderMass());
			}
		}
		if (propellant.kind() == PropellantProperties.PropellantKind.MACHINE_GUN_ROUND) {
			return getAutocannonPowderMassForProjectile(MACHINE_GUN_BULLET);
		}
		return propellant.resolvedPowderMassKg();
	}

	private static double getAutocannonPowderMassForProjectile(ResourceLocation projectileId) {
		ProjectileMassProperties properties = projectileId == null
				? null
				: ProjectileMassRegistry.getProperties(projectileId).orElse(null);
		return properties == null
				? BallisticsParameterRegistry.autocannonPowderMass()
				: properties.autocannonPowderMassKgOr(BallisticsParameterRegistry.autocannonPowderMass());
	}

	private static double estimateBigCartridgeChargeEquivalents(ItemStack stack) {
		int storedPower = stack.getOrDefault(CBCDataComponents.POWER, 0);
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
}
