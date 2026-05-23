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

public final class ItemProjectileRegistry {
	private static volatile Map<ResourceLocation, ResourceLocation> plainItemToProjectile = Map.of();
	private static volatile List<Mapping> itemToProjectile = List.of();

	private ItemProjectileRegistry() { }

	static void replaceAll(List<Mapping> newMappings) {
		Map<ResourceLocation, ResourceLocation> plain = new LinkedHashMap<>();
		List<Mapping> ordered = new ArrayList<>(newMappings);
		for (Mapping mapping : ordered) {
			if (mapping.selector().isPlainItemId()) plain.put(mapping.selector().itemId(), mapping.projectileId());
		}
		ordered.sort(Comparator
				.comparingInt((Mapping mapping) -> mapping.selector().specificity()).reversed()
				.thenComparing(Comparator.comparingInt(Mapping::loadOrder).reversed()));
		plainItemToProjectile = Collections.unmodifiableMap(plain);
		itemToProjectile = Collections.unmodifiableList(ordered);
	}

	public static Optional<ResourceLocation> getProjectileId(ResourceLocation itemId) {
		return Optional.ofNullable(plainItemToProjectile.get(itemId));
	}

	public static Optional<ResourceLocation> getProjectileId(ItemStack stack, HolderLookup.Provider registries) {
		if (stack == null || stack.isEmpty()) return Optional.empty();
		for (Mapping mapping : itemToProjectile) {
			if (mapping.selector().matches(stack, registries)) return Optional.of(mapping.projectileId());
		}
		return Optional.empty();
	}

	public record Mapping(ItemStackSelector selector, ResourceLocation projectileId, int loadOrder) { }
}
