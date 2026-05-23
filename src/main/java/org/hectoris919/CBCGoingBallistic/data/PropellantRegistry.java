package org.hectoris919.CBCGoingBallistic.data;

import net.minecraft.core.HolderLookup;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class PropellantRegistry {
	private static volatile Map<ResourceLocation, PropellantProperties> plainPropellants = Map.of();
	private static volatile List<Entry> propellants = List.of();

	private PropellantRegistry() { }

	static void replaceAll(List<Entry> newPropellants) {
		Map<ResourceLocation, PropellantProperties> plain = new LinkedHashMap<>();
		List<Entry> ordered = new ArrayList<>(newPropellants);
		for (Entry entry : ordered) {
			if (entry.selector().isPlainItemId()) plain.put(entry.selector().itemId(), entry.properties());
		}
		ordered.sort(Comparator
				.comparingInt((Entry entry) -> entry.selector().specificity()).reversed()
				.thenComparing(Comparator.comparingInt(Entry::loadOrder).reversed()));
		plainPropellants = Collections.unmodifiableMap(plain);
		propellants = Collections.unmodifiableList(ordered);
	}

	public static Optional<PropellantProperties> get(ResourceLocation itemId) {
		return Optional.ofNullable(plainPropellants.get(itemId));
	}

	public static Optional<PropellantProperties> get(ItemStack stack, HolderLookup.Provider registries) {
		if (stack == null || stack.isEmpty()) return Optional.empty();
		for (Entry entry : propellants) {
			if (entry.selector().matches(stack, registries)) return Optional.of(entry.properties());
		}
		return Optional.empty();
	}

	public record Entry(ItemStackSelector selector, PropellantProperties properties, int loadOrder) { }
}
