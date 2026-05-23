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

public class CannonComponentReloadListener extends SimpleJsonResourceReloadListener {
	private static final Gson GSON = new GsonBuilder().create();
	private static final String DIRECTORY = GoingBallistic.MODID + "/cannon_components";

	public CannonComponentReloadListener() { super(GSON, DIRECTORY); }
	public static void onAddReloadListeners(AddReloadListenerEvent event) { event.addListener(new CannonComponentReloadListener()); }

	@Override
	protected void apply(Map<ResourceLocation, JsonElement> files, @NotNull ResourceManager resourceManager, @NotNull ProfilerFiller profiler) {
		Map<ResourceLocation, CannonComponentProperties> loaded = new LinkedHashMap<>();
		int rejected = 0;
		for (Map.Entry<ResourceLocation, JsonElement> file : files.entrySet()) {
			try {
				JsonObject root = GsonHelper.convertToJsonObject(file.getValue(), file.getKey().toString());
				JsonObject values = GsonHelper.isObjectNode(root, "values")
						? GsonHelper.getAsJsonObject(root, "values")
						: root;
				for (Map.Entry<String, JsonElement> entry : values.entrySet()) {
					try {
						ResourceLocation blockId = parseId(file.getKey(), entry.getKey());
						JsonObject object = GsonHelper.convertToJsonObject(entry.getValue(), entry.getKey());
						double length = GsonHelper.getAsDouble(object, "length_m", 1.0D);
						double velocityMultiplier = GsonHelper.getAsDouble(object, "velocity_multiplier", 1.0D);
						try {
							if (BuiltInRegistries.BLOCK.getOptional(blockId).isEmpty()) GoingBallistic.LOGGER.warn("Cannon component entry {} in {} does not match a registered block. Loading it anyway for optional addon compatibility.", blockId, file.getKey());
						} catch (RuntimeException ignored) { }
						loaded.put(blockId, new CannonComponentProperties(length, velocityMultiplier));
					} catch (IllegalArgumentException | JsonParseException ex) {
						++rejected;
						GoingBallistic.LOGGER.error("Could not parse cannon component entry {} in {}", entry.getKey(), file.getKey(), ex);
					}
				}
			} catch (IllegalArgumentException | JsonParseException ex) {
				++rejected;
				GoingBallistic.LOGGER.error("Could not parse cannon component file {}", file.getKey(), ex);
			}
		}
		CannonComponentRegistry.replaceAll(loaded);
		GoingBallistic.LOGGER.info("Loaded {} Going Ballistic cannon component entries{}", loaded.size(), rejected > 0 ? "; rejected " + rejected : "");
	}

	private static ResourceLocation parseId(ResourceLocation fileId, String rawId) {
		try {
			return ResourceLocation.parse(rawId);
		} catch (IllegalArgumentException ex) {
			throw new JsonParseException("Invalid block id '" + rawId + "' in " + fileId, ex);
		}
	}
}
