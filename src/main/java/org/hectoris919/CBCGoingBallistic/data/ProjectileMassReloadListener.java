package org.hectoris919.CBCGoingBallistic.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import org.hectoris919.CBCGoingBallistic.GoingBallistic;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public class ProjectileMassReloadListener extends SimpleJsonResourceReloadListener {
	private static final Gson GSON = new GsonBuilder().create();
	private static final String DIRECTORY = GoingBallistic.MODID + "/projectile_masses";

	public ProjectileMassReloadListener() { super(GSON, DIRECTORY); }

	public static void onAddReloadListeners(AddReloadListenerEvent event) { event.addListener(new ProjectileMassReloadListener()); }

	@Override
	protected void apply(Map<ResourceLocation, JsonElement> files, @NotNull ResourceManager resourceManager, @NotNull ProfilerFiller profiler) {
		Map<ResourceLocation, ProjectileMassProperties> loaded = new LinkedHashMap<>();
		int rejected = 0;

		for (Map.Entry<ResourceLocation, JsonElement> file : files.entrySet()) {
			try {
				JsonObject root = GsonHelper.convertToJsonObject(file.getValue(), file.getKey().toString());
				if (GsonHelper.isObjectNode(root, "values")) {
					rejected += parseValuesObject(file.getKey(), GsonHelper.getAsJsonObject(root, "values"), loaded);
				} else if (GsonHelper.isObjectNode(root, "projectiles")) {
					rejected += parseValuesObject(file.getKey(), GsonHelper.getAsJsonObject(root, "projectiles"), loaded);
				} else {
					rejected += parseSingleObject(file.getKey(), root, loaded);
				}
			} catch (IllegalArgumentException | JsonParseException ex) {
				++rejected;
				GoingBallistic.LOGGER.error("Could not parse projectile mass file {}", file.getKey(), ex);
			}
		}

		ProjectileMassRegistry.replaceAll(loaded);
		if (rejected > 0) {
			GoingBallistic.LOGGER.warn("Loaded {} Going Ballistic projectile mass entries; rejected {} invalid entries/files", loaded.size(), rejected);
		} else {
			GoingBallistic.LOGGER.info("Loaded {} Going Ballistic projectile mass entries", loaded.size());
		}
	}

	private static int parseValuesObject(ResourceLocation fileId, JsonObject values, Map<ResourceLocation, ProjectileMassProperties> loaded) {
		int rejected = 0;
		for (Map.Entry<String, JsonElement> entry : values.entrySet()) {
			try {
				ResourceLocation projectileId = parseProjectileId(fileId, entry.getKey());
				JsonObject value = GsonHelper.convertToJsonObject(entry.getValue(), entry.getKey());
				Optional<ProjectileMassProperties> properties = parseProperties(fileId, projectileId, value);
				properties.ifPresent(props -> putEntry(fileId, projectileId, props, loaded));
				if (properties.isEmpty()) {
					++rejected;
				}
			} catch (IllegalArgumentException | JsonParseException ex) {
				++rejected;
				GoingBallistic.LOGGER.error("Could not parse projectile mass entry {} in {}", entry.getKey(), fileId, ex);
			}
		}
		return rejected;
	}

	private static int parseSingleObject(ResourceLocation fileId, JsonObject root, Map<ResourceLocation, ProjectileMassProperties> loaded) {
		ResourceLocation projectileId = parseProjectileId(fileId, GsonHelper.getAsString(root, "projectile"));
		Optional<ProjectileMassProperties> properties = parseProperties(fileId, projectileId, root);
		properties.ifPresent(props -> putEntry(fileId, projectileId, props, loaded));
		return properties.isPresent()
				? 0
				: 1;
	}

	private static ResourceLocation parseProjectileId(ResourceLocation fileId, String rawId) {
		try {
			return ResourceLocation.parse(rawId);
		} catch (IllegalArgumentException ex) {
			throw new JsonParseException("Invalid projectile id '" + rawId + "' in " + fileId, ex);
		}
	}

	private static void putEntry(ResourceLocation fileId, ResourceLocation projectileId, ProjectileMassProperties properties, Map<ResourceLocation, ProjectileMassProperties> loaded) {
		try {
			if (BuiltInRegistries.ENTITY_TYPE.getOptional(projectileId).isEmpty()) {
				GoingBallistic.LOGGER.warn("Projectile mass entry {} in {} does not currently match a registered entity type. Loading it anyway for optional addon compatibility.", projectileId, fileId);
			}
		} catch (RuntimeException ignored) { }

		ProjectileMassProperties previous = loaded.put(projectileId, properties);
		if (previous != null) GoingBallistic.LOGGER.warn("Projectile mass entry {} in {} overrides an earlier entry in this reload", projectileId, fileId);

	}

	private static Optional<ProjectileMassProperties> parseProperties(ResourceLocation fileId, ResourceLocation projectileId, JsonObject json) {
		Double autocannonPowderMassKg = readOptionalPositive(fileId, projectileId, json, "autocannon_powder_mass");
		Double autocannonChargeLengthMeters = readOptionalPositive(fileId, projectileId, json, "autocannon_charge_diameter");
		Double autocannonVelocityMultiplier = readOptionalPositive(fileId, projectileId, json, "autocannon_velocity_multiplier");
		Double maxSafeChargeEquivalents = readOptionalPositive(fileId, projectileId, json, "max_safe_charge_equivalents");

		if (GsonHelper.isNumberValue(json, "mass")) {
			double massKg = GsonHelper.getAsDouble(json, "mass");
			if (isNegativeOrInfinite(fileId, projectileId, "mass", massKg)) return Optional.empty();
			return Optional.of(ProjectileMassProperties.fixed(massKg, autocannonPowderMassKg, autocannonChargeLengthMeters, autocannonVelocityMultiplier, maxSafeChargeEquivalents));
		}

		if (GsonHelper.isNumberValue(json, "empty_mass") && GsonHelper.isNumberValue(json, "internal_fluid_volume_m3")) {
			double emptyMassKg = GsonHelper.getAsDouble(json, "empty_mass");
			double internalVolumeM3 = GsonHelper.getAsDouble(json, "internal_fluid_volume_m3");
			double referenceDensity = GsonHelper.getAsDouble(json, "reference_fluid_density_kg_per_m3", 1000.0D);

			if (isNegativeOrInfinite(fileId, projectileId, "empty_mass", emptyMassKg)) return Optional.empty();
			if (isNegativeOrInfinite(fileId, projectileId, "internal_fluid_volume_m3", internalVolumeM3)) return Optional.empty();
			if (isNegativeOrInfinite(fileId, projectileId, "reference_fluid_density_kg_per_m3", referenceDensity)) return Optional.empty();

			return Optional.of(ProjectileMassProperties.fluidContainer(emptyMassKg, internalVolumeM3, referenceDensity, autocannonPowderMassKg, autocannonChargeLengthMeters, autocannonVelocityMultiplier, maxSafeChargeEquivalents));
		}

		GoingBallistic.LOGGER.warn("Projectile mass entry {} in {} must define either mass or empty_mass + internal_fluid_volume_m3", projectileId, fileId);
		return Optional.empty();
	}

	private static Double readOptionalPositive(ResourceLocation fileId, ResourceLocation projectileId, JsonObject json, String key) {
		if (!json.has(key)) return null;
		double value = GsonHelper.getAsDouble(json, key);
		if (isNegativeOrInfinite(fileId, projectileId, key, value)) return null;
		return value;
	}

	private static boolean isNegativeOrInfinite(ResourceLocation fileId, ResourceLocation projectileId, String key, double value) {
		if (!Double.isFinite(value) || value < 0.0D) {
			GoingBallistic.LOGGER.warn("Ignoring projectile mass entry {} in {} because {}={} is not positive and finite", projectileId, fileId, key, value);
		}
		return !Double.isFinite(value) || value < 0.0D;
	}
}
