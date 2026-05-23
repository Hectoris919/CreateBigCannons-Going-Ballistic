package org.hectoris919.CBCGoingBallistic.data;

import net.minecraft.resources.ResourceLocation;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class CannonComponentRegistry {
	private static volatile Map<ResourceLocation, CannonComponentProperties> components = Map.of();

	private CannonComponentRegistry() { }

	static void replaceAll(Map<ResourceLocation, CannonComponentProperties> newComponents) {
		components = Collections.unmodifiableMap(new LinkedHashMap<>(newComponents));
	}

	public static Optional<CannonComponentProperties> get(ResourceLocation blockId) {
		return Optional.ofNullable(components.get(blockId));
	}
}
