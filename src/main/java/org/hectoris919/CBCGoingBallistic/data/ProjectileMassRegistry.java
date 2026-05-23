package org.hectoris919.CBCGoingBallistic.data;

import net.minecraft.resources.ResourceLocation;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class ProjectileMassRegistry {
	private static volatile Map<ResourceLocation, ProjectileMassProperties> projectileMasses = Map.of();

	private ProjectileMassRegistry() { }

	static void replaceAll(Map<ResourceLocation, ProjectileMassProperties> newMasses) {
		projectileMasses = Collections.unmodifiableMap(new LinkedHashMap<>(newMasses));
	}

	public static Optional<ProjectileMassProperties> getProperties(ResourceLocation projectileId) {
		return Optional.ofNullable(projectileMasses.get(projectileId));
	}
}
