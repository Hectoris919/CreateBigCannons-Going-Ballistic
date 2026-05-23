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

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class ProjectileMassReloadListener extends SimpleJsonResourceReloadListener {
	private static final Gson GSON = new GsonBuilder().create();
	private static final String DIRECTORY = GoingBallistic.MODID + "/projectile_masses";

	public ProjectileMassReloadListener() { super(GSON, DIRECTORY); }

	public static void onAddReloadListeners(AddReloadListenerEvent event) { event.addListener(new ProjectileMassReloadListener()); }

	@Override
	protected void apply(Map<ResourceLocation, JsonElement> files, @NotNull ResourceManager resourceManager, @NotNull ProfilerFiller profiler) {
		Map<ResourceLocation, RawEntry> rawEntries = new LinkedHashMap<>();
		int rejected = 0;

		for (Map.Entry<ResourceLocation, JsonElement> file : files.entrySet()) {
			try {
				JsonObject root = GsonHelper.convertToJsonObject(file.getValue(), file.getKey().toString());
				if (GsonHelper.isObjectNode(root, "values")) {
					rejected += parseValuesObject(file.getKey(), GsonHelper.getAsJsonObject(root, "values"), rawEntries);
				} else if (GsonHelper.isObjectNode(root, "projectiles")) {
					rejected += parseValuesObject(file.getKey(), GsonHelper.getAsJsonObject(root, "projectiles"), rawEntries);
				} else {
					rejected += parseSingleObject(file.getKey(), root, rawEntries);
				}
			} catch (IllegalArgumentException | JsonParseException ex) {
				++rejected;
				GoingBallistic.LOGGER.error("Could not parse projectile mass file {}", file.getKey(), ex);
			}
		}

		Map<ResourceLocation, ProjectileMassProperties> loaded = new LinkedHashMap<>();
		for (ResourceLocation projectileId : rawEntries.keySet()) {
			Optional<ProjectileMassProperties> properties = resolve(projectileId, rawEntries, loaded, new HashSet<>());
			if (properties.isEmpty()) ++rejected;
		}

		ProjectileMassRegistry.replaceAll(loaded);
		if (rejected > 0) {
			GoingBallistic.LOGGER.warn("Loaded {} Going Ballistic projectile mass entries; rejected {} invalid entries/files", loaded.size(), rejected);
		} else {
			GoingBallistic.LOGGER.info("Loaded {} Going Ballistic projectile mass entries", loaded.size());
		}
	}

	private static int parseValuesObject(ResourceLocation fileId, JsonObject values, Map<ResourceLocation, RawEntry> rawEntries) {
		int rejected = 0;
		for (Map.Entry<String, JsonElement> entry : values.entrySet()) {
			try {
				ResourceLocation projectileId = parseProjectileId(fileId, entry.getKey());
				JsonObject value = GsonHelper.convertToJsonObject(entry.getValue(), entry.getKey());
				putRawEntry(fileId, projectileId, value, rawEntries);
			} catch (IllegalArgumentException | JsonParseException ex) {
				++rejected;
				GoingBallistic.LOGGER.error("Could not parse projectile mass entry {} in {}", entry.getKey(), fileId, ex);
			}
		}
		return rejected;
	}

	private static int parseSingleObject(ResourceLocation fileId, JsonObject root, Map<ResourceLocation, RawEntry> rawEntries) {
		ResourceLocation projectileId = parseProjectileId(fileId, GsonHelper.getAsString(root, "projectile"));
		putRawEntry(fileId, projectileId, root, rawEntries);
		return 0;
	}

	private static void putRawEntry(ResourceLocation fileId, ResourceLocation projectileId, JsonObject value, Map<ResourceLocation, RawEntry> rawEntries) {
		try {
			if (BuiltInRegistries.ENTITY_TYPE.getOptional(projectileId).isEmpty()) {
				GoingBallistic.LOGGER.warn("Projectile mass entry {} in {} does not currently match a registered entity type. Loading it anyway for optional addon compatibility.", projectileId, fileId);
			}
		} catch (RuntimeException ignored) { }
		RawEntry previous = rawEntries.put(projectileId, new RawEntry(fileId, value));
		if (previous != null) GoingBallistic.LOGGER.warn("Projectile mass entry {} in {} overrides an earlier entry in {}", projectileId, fileId, previous.fileId());
	}

	private static Optional<ProjectileMassProperties> resolve(ResourceLocation projectileId, Map<ResourceLocation, RawEntry> rawEntries, Map<ResourceLocation, ProjectileMassProperties> loaded, Set<ResourceLocation> resolving) {
		ProjectileMassProperties alreadyLoaded = loaded.get(projectileId);
		if (alreadyLoaded != null) return Optional.of(alreadyLoaded);

		RawEntry raw = rawEntries.get(projectileId);
		if (raw == null) return Optional.empty();
		if (!resolving.add(projectileId)) {
			GoingBallistic.LOGGER.error("Projectile mass copy cycle detected while resolving {}", projectileId);
			return Optional.empty();
		}

		Optional<ProjectileMassProperties> parsed = parseProperties(raw.fileId(), projectileId, raw.json(), rawEntries, loaded, resolving);
		resolving.remove(projectileId);
		parsed.ifPresent(properties -> loaded.put(projectileId, properties));
		return parsed;
	}

	private static Optional<ProjectileMassProperties> parseProperties(ResourceLocation fileId, ResourceLocation projectileId, JsonObject json, Map<ResourceLocation, RawEntry> rawEntries, Map<ResourceLocation, ProjectileMassProperties> loaded, Set<ResourceLocation> resolving) {
		Double autocannonPowderMassKg = readOptionalPositive(fileId, projectileId, json, "autocannon_powder_mass");
		Double autocannonChargeLengthMeters = readOptionalPositive(fileId, projectileId, json, "autocannon_charge_length");
		Double autocannonVelocityMultiplier = readOptionalPositive(fileId, projectileId, json, "autocannon_velocity_multiplier");
		Double maxSafeChargeEquivalents = readOptionalPositive(fileId, projectileId, json, "max_safe_charge_equivalents");
		double massMultiplier = GsonHelper.getAsDouble(json, "mass_multiplier", 1.0D);
		double massOffset = GsonHelper.getAsDouble(json, "mass_offset", 0.0D);

		if (GsonHelper.isStringValue(json, "copy")) {
			ResourceLocation copiedId = parseProjectileId(fileId, GsonHelper.getAsString(json, "copy"));
			ProjectileMassProperties copied = loaded.get(copiedId);
			if (copied == null) copied = resolve(copiedId, rawEntries, loaded, resolving).orElse(null);
			if (copied == null) {
				GoingBallistic.LOGGER.warn("Projectile mass entry {} in {} copies {}, but that entry could not be resolved", projectileId, fileId, copiedId);
				return Optional.empty();
			}
			return Optional.of(copied.withMassModifiers(massMultiplier, massOffset).withLaunchOverrides(autocannonPowderMassKg, autocannonChargeLengthMeters, autocannonVelocityMultiplier, maxSafeChargeEquivalents));
		}

		ProjectileMassProperties base;
		String type = GsonHelper.getAsString(json, "type", "");
		if ("container".equals(type) || GsonHelper.isNumberValue(json, "max_children") || GsonHelper.isStringValue(json, "child_item_list_path")) {
			double emptyMassKg = GsonHelper.getAsDouble(json, "empty_mass", GsonHelper.getAsDouble(json, "mass", 0.001D));
			int maxChildren = GsonHelper.getAsInt(json, "max_children", 0);
			boolean allowRecursive = GsonHelper.getAsBoolean(json, "allow_recursive", false);
			String childPath = GsonHelper.getAsString(json, "child_item_list_path", "Shells");
			double childFallbackMassKg = GsonHelper.getAsDouble(json, "child_fallback_mass", BallisticsParameterRegistry.projectileMassFallback());
			if (isNegativeOrInfinite(fileId, projectileId, "empty_mass", emptyMassKg)) return Optional.empty();
			base = ProjectileMassProperties.projectileContainer(emptyMassKg, maxChildren, allowRecursive, childPath, childFallbackMassKg, maxSafeChargeEquivalents);
		} else if (GsonHelper.isNumberValue(json, "mass")) {
			double massKg = GsonHelper.getAsDouble(json, "mass");
			if (isNegativeOrInfinite(fileId, projectileId, "mass", massKg)) return Optional.empty();
			base = ProjectileMassProperties.fixed(massKg, autocannonPowderMassKg, autocannonChargeLengthMeters, autocannonVelocityMultiplier, maxSafeChargeEquivalents);
		} else if (GsonHelper.isNumberValue(json, "empty_mass") && GsonHelper.isNumberValue(json, "internal_fluid_volume_m3")) {
			double emptyMassKg = GsonHelper.getAsDouble(json, "empty_mass");
			double internalVolumeM3 = GsonHelper.getAsDouble(json, "internal_fluid_volume_m3");
			double referenceDensity = GsonHelper.getAsDouble(json, "reference_fluid_density_kg_per_m3", 1000.0D);

			if (isNegativeOrInfinite(fileId, projectileId, "empty_mass", emptyMassKg)) return Optional.empty();
			if (isNegativeOrInfinite(fileId, projectileId, "internal_fluid_volume_m3", internalVolumeM3)) return Optional.empty();
			if (isNegativeOrInfinite(fileId, projectileId, "reference_fluid_density_kg_per_m3", referenceDensity)) return Optional.empty();

			base = ProjectileMassProperties.fluidContainer(emptyMassKg, internalVolumeM3, referenceDensity, autocannonPowderMassKg, autocannonChargeLengthMeters, autocannonVelocityMultiplier, maxSafeChargeEquivalents);
		} else {
			GoingBallistic.LOGGER.warn("Projectile mass entry {} in {} must define mass, copy, empty_mass + internal_fluid_volume_m3, or type=container", projectileId, fileId);
			return Optional.empty();
		}

		return Optional.of(base.withMassModifiers(massMultiplier, massOffset).withLaunchOverrides(autocannonPowderMassKg, autocannonChargeLengthMeters, autocannonVelocityMultiplier, maxSafeChargeEquivalents));
	}

	private static ResourceLocation parseProjectileId(ResourceLocation fileId, String rawId) {
		try {
			return ResourceLocation.parse(rawId);
		} catch (IllegalArgumentException ex) {
			throw new JsonParseException("Invalid projectile id '" + rawId + "' in " + fileId, ex);
		}
	}

	private static Double readOptionalPositive(ResourceLocation fileId, ResourceLocation projectileId, JsonObject json, String key) {
		if (!json.has(key)) return null;
		double value = GsonHelper.getAsDouble(json, key);
		if (isNegativeOrInfinite(fileId, projectileId, key, value) || value <= 0.0D) return null;
		return value;
	}

	private static boolean isNegativeOrInfinite(ResourceLocation fileId, ResourceLocation projectileId, String key, double value) {
		if (!Double.isFinite(value) || value < 0.0D) {
			GoingBallistic.LOGGER.warn("Ignoring projectile mass entry {} in {} because {}={} is not non-negative and finite", projectileId, fileId, key, value);
		}
		return !Double.isFinite(value) || value < 0.0D;
	}

	private record RawEntry(ResourceLocation fileId, JsonObject json) { }
}
