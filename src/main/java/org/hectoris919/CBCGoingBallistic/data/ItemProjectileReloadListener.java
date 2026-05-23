package org.hectoris919.CBCGoingBallistic.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import org.hectoris919.CBCGoingBallistic.GoingBallistic;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class ItemProjectileReloadListener extends SimpleJsonResourceReloadListener {
	private static final Gson GSON = new GsonBuilder().create();
	private static final String DIRECTORY = GoingBallistic.MODID + "/item_projectiles";

	public ItemProjectileReloadListener() { super(GSON, DIRECTORY); }
	public static void onAddReloadListeners(AddReloadListenerEvent event) { event.addListener(new ItemProjectileReloadListener()); }

	@Override
	protected void apply(java.util.Map<ResourceLocation, JsonElement> files, @NotNull ResourceManager resourceManager, @NotNull ProfilerFiller profiler) {
		List<ItemProjectileRegistry.Mapping> loaded = new ArrayList<>();
		Set<String> seenSelectors = new LinkedHashSet<>();
		int rejected = 0;
		int loadOrder = 0;
		for (java.util.Map.Entry<ResourceLocation, JsonElement> file : files.entrySet()) {
			try {
				JsonObject root = GsonHelper.convertToJsonObject(file.getValue(), file.getKey().toString());
				JsonObject values = GsonHelper.isObjectNode(root, "values") ? GsonHelper.getAsJsonObject(root, "values") : root;
				for (java.util.Map.Entry<String, JsonElement> entry : values.entrySet()) {
					try {
						ItemStackSelector selector = parseSelector(file.getKey(), entry.getKey());
						ResourceLocation projectileId;
						if (entry.getValue().isJsonPrimitive()) {
							projectileId = parseId(file.getKey(), entry.getValue().getAsString());
						} else {
							JsonObject object = GsonHelper.convertToJsonObject(entry.getValue(), entry.getKey());
							projectileId = parseId(file.getKey(), GsonHelper.getAsString(object, "projectile"));
						}
						warnIfUnregistered(file.getKey(), selector.itemId(), projectileId);
						if (!seenSelectors.add(selector.sourceText())) GoingBallistic.LOGGER.warn("Item projectile selector {} in {} overrides an earlier selector", selector, file.getKey());
						loaded.add(new ItemProjectileRegistry.Mapping(selector, projectileId, loadOrder++));
					} catch (IllegalArgumentException | JsonParseException | CommandSyntaxException ex) {
						++rejected;
						GoingBallistic.LOGGER.error("Could not parse item projectile mapping {} in {}", entry.getKey(), file.getKey(), ex);
					}
				}
			} catch (IllegalArgumentException | JsonParseException ex) {
				++rejected;
				GoingBallistic.LOGGER.error("Could not parse item projectile file {}", file.getKey(), ex);
			}
		}
		ItemProjectileRegistry.replaceAll(loaded);
		GoingBallistic.LOGGER.info("Loaded {} Going Ballistic item projectile mappings{}", loaded.size(), rejected > 0 ? "; rejected " + rejected : "");
	}

	private static ItemStackSelector parseSelector(ResourceLocation fileId, String rawSelector) throws CommandSyntaxException {
		try {
			return ItemStackSelector.parse(rawSelector);
		} catch (IllegalArgumentException ex) {
			throw new JsonParseException("Invalid item selector '" + rawSelector + "' in " + fileId, ex);
		}
	}

	private static ResourceLocation parseId(ResourceLocation fileId, String rawId) {
		try {
			return ResourceLocation.parse(rawId);
		} catch (IllegalArgumentException ex) {
			throw new JsonParseException("Invalid projectile id '" + rawId + "' in " + fileId, ex);
		}
	}

	private static void warnIfUnregistered(ResourceLocation fileId, ResourceLocation itemId, ResourceLocation projectileId) {
		try {
			if (BuiltInRegistries.ITEM.getOptional(itemId).isEmpty()) GoingBallistic.LOGGER.warn("Item projectile mapping {} in {} does not match a registered item. Loading it anyway for optional addon compatibility.", itemId, fileId);
			if (BuiltInRegistries.ENTITY_TYPE.getOptional(projectileId).isEmpty()) GoingBallistic.LOGGER.warn("Item projectile mapping {} -> {} in {} does not match a registered entity type. Loading it anyway for optional addon compatibility.", itemId, projectileId, fileId);
		} catch (RuntimeException ignored) { }
	}
}
