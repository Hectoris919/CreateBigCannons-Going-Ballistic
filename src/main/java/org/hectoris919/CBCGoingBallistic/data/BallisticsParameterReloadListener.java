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
		count += readPositive(json, fileId, "cannon_powder_mass", builder::cannonPowderMass);
		count += readPositive(json, fileId, "cannon_charge_diameter", builder::cannonChargeDiameter);
		count += readPositive(json, fileId, "cannon_charge_length", builder::cannonChargeLength);
		count += readPositive(json, fileId, "autocannon_powder_mass", builder::autocannonPowderMass);
		count += readPositive(json, fileId, "autocannon_charge_diameter", builder::autocannonCartridgeDiameter);
		count += readPositive(json, fileId, "autocannon_charge_length", builder::autocannonCartridgeLength);
		count += readPositive(json, fileId, "black_powder_energy_j_per_kg", builder::blackPowderEnergyJoulesPerKg);
		return count;
	}

	private static int readPositive(JsonObject json, ResourceLocation fileId, String key, DoubleConsumer setter) {
		if (!json.has(key)) return 0;

		double value = GsonHelper.getAsDouble(json, key);
		if (!Double.isFinite(value) || value <= 0.0D) {
			GoingBallistic.LOGGER.warn("Ignoring {}={} in {} because it must be positive and finite", key, value, fileId);
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
