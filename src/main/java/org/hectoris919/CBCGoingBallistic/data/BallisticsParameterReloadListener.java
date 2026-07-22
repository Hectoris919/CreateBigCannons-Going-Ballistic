package org.hectoris919.CBCGoingBallistic.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import org.hectoris919.CBCGoingBallistic.GoingBallistic;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class BallisticsParameterReloadListener extends SimpleJsonResourceReloadListener {
	private static final Gson GSON = new GsonBuilder().create();
	private static final String DIRECTORY = GoingBallistic.MODID + "/ballistics_parameters";

	public BallisticsParameterReloadListener() {
		super(GSON, DIRECTORY);
	}

	public static void onAddReloadListeners(AddReloadListenerEvent event) { event.addListener(new BallisticsParameterReloadListener()); }

	@Override
	protected void apply(Map<ResourceLocation, JsonElement> files, @NotNull ResourceManager resourceManager, @NotNull ProfilerFiller profiler) {
		BallisticsParameterRegistry.Builder builder = new BallisticsParameterRegistry.Builder();
		int loadedFields = 0;

		for (Map.Entry<ResourceLocation, JsonElement> file : files.entrySet()) {
			try {
				JsonObject root = GsonHelper.convertToJsonObject(file.getValue(), file.getKey().toString());
				JsonObject values = GsonHelper.isObjectNode(root, "values")
						? GsonHelper.getAsJsonObject(root, "values")
						: root;
				loadedFields += parseParameterFile(file.getKey(), values, builder);
			} catch (IllegalArgumentException | JsonParseException ex) {
				GoingBallistic.LOGGER.error("Could not parse Going Ballistic ballistics parameter file {}", file.getKey(), ex);
			}
		}

		BallisticsParameterRegistry.Overrides overrides = builder.build();
		BallisticsParameterRegistry.replace(overrides);
		if (overrides.isEmpty()) {
			GoingBallistic.LOGGER.info("Loaded 0 Going Ballistic datapack ballistics parameter overrides");
		} else {
			GoingBallistic.LOGGER.info("Loaded {} Going Ballistic datapack ballistics parameter override fields", loadedFields);
		}
	}

	private static int parseParameterFile(ResourceLocation fileId, JsonObject json, BallisticsParameterRegistry.Builder builder) {
		int count = 0;
		count += readPositive(json, fileId, "robins_constant_mps", builder::robinsConstantMps);
		count += readPositive(json, fileId, "velocity_multiplier", builder::velocityMultiplier);
		count += readPositive(json, fileId, "projectile_mass_fallback", builder::projectileMassFallback);
		count += readPositive(json, fileId, "autocannon_projectile_mass_fallback", builder::autocannonProjectileMassFallback);
		count += readPositive(json, fileId, "cannon_powder_mass", builder::cannonPowderMass);
		count += readPositive(json, fileId, "cannon_charge_diameter", builder::cannonChargeDiameter);
		count += readPositive(json, fileId, "cannon_charge_length", builder::cannonChargeLength);
		count += readPositive(json, fileId, "autocannon_powder_mass", builder::autocannonPowderMass);
		count += readPositive(json, fileId, "autocannon_charge_diameter", builder::autocannonCartridgeDiameter);
		count += readPositive(json, fileId, "autocannon_charge_length", builder::autocannonCartridgeLength);
		count += readPositive(json, fileId, "black_powder_energy_j_per_kg", builder::blackPowderEnergyJoulesPerKg);
		count += readPositive(json, fileId, "air_density", builder::airDensity);
		count += readPositive(json, fileId, "projectile_drag_coefficient", builder::projectileDragCoefficient);
		count += readPositive(json, fileId, "machine_gun_bullet_radius", builder::machineGunBulletRadius);
		count += readPositive(json, fileId, "joules_per_toughness_point", builder::joulesPerToughnessPoint);
		count += readPositive(json, fileId, "kilograms_per_sable_mass_unit", builder::kilogramsPerSableMassUnit);
		count += readNonNegative(json, fileId, "sable_recoil_gas_velocity_multiplier", builder::sableRecoilGasVelocityMultiplier);
		count += readNonNegative(json, fileId, "sable_blank_gas_velocity_mps", builder::sableBlankGasVelocityMps);
		count += readNonNegative(json, fileId, "sable_blank_gas_impulse_efficiency", builder::sableBlankGasImpulseEfficiency);
		count += readNonNegative(json, fileId, "sable_recoil_impulse_scale", builder::sableRecoilImpulseScale);
		count += readNonNegative(json, fileId, "sable_impact_impulse_scale", builder::sableImpactImpulseScale);
		warnDeprecatedParameter(json, fileId, "autocannon_joules_per_block_damage_point");
		warnDeprecatedParameter(json, fileId, "machine_gun_joules_per_block_damage_point");
		warnDeprecatedParameter(json, fileId, "min_hard_target_damage_factor");
		warnDeprecatedParameter(json, fileId, "ap_projectile_block_damage_multiplier");
		warnDeprecatedParameter(json, fileId, "small_arms_block_damage_multiplier");
		warnDeprecatedSableForceDuration(json, fileId);
		return count;
	}

	private static void warnDeprecatedSableForceDuration(JsonObject json, ResourceLocation fileId) {
		if (json.has("sable_force_duration_seconds")) {
			GoingBallistic.LOGGER.warn("Ignoring deprecated Going Ballistic parameter sable_force_duration_seconds in {}; Sable's queued point-force API applies the supplied vector directly as an impulse", fileId);
		}
	}

	private static void warnDeprecatedParameter(JsonObject json, ResourceLocation fileId, String key) {
		if (json.has(key)) {
			GoingBallistic.LOGGER.warn("Ignoring deprecated Going Ballistic parameter {} in {}; realistic block damage now uses only joules_per_toughness_point", key, fileId);
		}
	}

	private static int readPositive(JsonObject json, ResourceLocation fileId, String key, DoubleConsumer setter) {
		return readNumber(json, fileId, key, true, setter);
	}

	private static int readNonNegative(JsonObject json, ResourceLocation fileId, String key, DoubleConsumer setter) {
		return readNumber(json, fileId, key, false, setter);
	}

	private static int readNumber(JsonObject json, ResourceLocation fileId, String key, boolean strictlyPositive, DoubleConsumer setter) {
		if (!json.has(key)) return 0;

		double value = GsonHelper.getAsDouble(json, key);
		boolean invalid = !Double.isFinite(value) || (strictlyPositive ? value <= 0.0D : value < 0.0D);
		if (invalid) {
			GoingBallistic.LOGGER.warn("Ignoring {}={} in {} because it must be {} and finite", key, value, fileId, strictlyPositive ? "positive" : "non-negative");
			return 0;
		}
		setter.accept(value);
		return 1;
	}

	@FunctionalInterface
	private interface DoubleConsumer {
		void accept(double value);
	}
}