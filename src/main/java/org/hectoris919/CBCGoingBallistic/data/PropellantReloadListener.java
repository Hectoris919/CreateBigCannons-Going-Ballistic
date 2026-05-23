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

public class PropellantReloadListener extends SimpleJsonResourceReloadListener {
	private static final Gson GSON = new GsonBuilder().create();
	private static final String DIRECTORY = GoingBallistic.MODID + "/propellant_items";

	public PropellantReloadListener() { super(GSON, DIRECTORY); }
	public static void onAddReloadListeners(AddReloadListenerEvent event) { event.addListener(new PropellantReloadListener()); }

	@Override
	protected void apply(java.util.Map<ResourceLocation, JsonElement> files, @NotNull ResourceManager resourceManager, @NotNull ProfilerFiller profiler) {
		List<PropellantRegistry.Entry> loaded = new ArrayList<>();
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
						JsonObject object = GsonHelper.convertToJsonObject(entry.getValue(), entry.getKey());
						PropellantProperties properties = parseProperties(object);
						try {
							if (BuiltInRegistries.ITEM.getOptional(selector.itemId()).isEmpty()) GoingBallistic.LOGGER.warn("Propellant entry {} in {} does not match a registered item. Loading it anyway for optional addon compatibility.", selector.itemId(), file.getKey());
						} catch (RuntimeException ignored) { }
						if (!seenSelectors.add(selector.sourceText())) GoingBallistic.LOGGER.warn("Propellant selector {} in {} overrides an earlier selector", selector, file.getKey());
						loaded.add(new PropellantRegistry.Entry(selector, properties, loadOrder++));
					} catch (IllegalArgumentException | JsonParseException | CommandSyntaxException ex) {
						++rejected;
						GoingBallistic.LOGGER.error("Could not parse propellant entry {} in {}", entry.getKey(), file.getKey(), ex);
					}
				}
			} catch (IllegalArgumentException | JsonParseException ex) {
				++rejected;
				GoingBallistic.LOGGER.error("Could not parse propellant file {}", file.getKey(), ex);
			}
		}
		PropellantRegistry.replaceAll(loaded);
		GoingBallistic.LOGGER.info("Loaded {} Going Ballistic propellant entries{}", loaded.size(), rejected > 0 ? "; rejected " + rejected : "");
	}

	private static PropellantProperties parseProperties(JsonObject object) {
		PropellantProperties.PropellantKind kind = PropellantProperties.PropellantKind.parse(GsonHelper.getAsString(object, "type", "autocannon_cartridge"));
		double chargeEquivalents = GsonHelper.getAsDouble(object, "charge_equivalents", kind == PropellantProperties.PropellantKind.BIG_CANNON_CHARGE ? 1.0D : 0.0D);
		Double powderMass = optionalDouble(object, "powder_mass");
		Double chargeLength = optionalDouble(object, "charge_length");
		double velocityMultiplier = GsonHelper.getAsDouble(object, "velocity_multiplier", 1.0D);
		double caseMass = GsonHelper.getAsDouble(object, "case_mass", 0.0D);
		return new PropellantProperties(kind, chargeEquivalents, powderMass, chargeLength, velocityMultiplier, caseMass);
	}

	private static Double optionalDouble(JsonObject object, String key) {
		return object.has(key) ? GsonHelper.getAsDouble(object, key) : null;
	}

	private static ItemStackSelector parseSelector(ResourceLocation fileId, String rawSelector) throws CommandSyntaxException {
		try {
			return ItemStackSelector.parse(rawSelector);
		} catch (IllegalArgumentException ex) {
			throw new JsonParseException("Invalid item selector '" + rawSelector + "' in " + fileId, ex);
		}
	}
}
