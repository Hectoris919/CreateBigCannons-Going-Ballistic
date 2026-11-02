package org.hectoris919.CBCGoingBallistic.ballistics;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

import org.hectoris919.CBCGoingBallistic.Config;
import org.hectoris919.CBCGoingBallistic.ballistics.BlockDamageMath.ImpactSolution;
import org.hectoris919.CBCGoingBallistic.data.BallisticsParameterRegistry;
import org.hectoris919.CBCGoingBallistic.mixin.AbstractCannonProjectileCompatAccessor;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import rbasamoyai.createbigcannons.block_armor_properties.BlockArmorPropertiesHandler;
import rbasamoyai.createbigcannons.block_armor_properties.BlockArmorPropertiesProvider;
import rbasamoyai.createbigcannons.config.CBCCfgMunitions;
import rbasamoyai.createbigcannons.munitions.AbstractCannonProjectile;
import rbasamoyai.createbigcannons.munitions.ProjectileContext;
import rbasamoyai.createbigcannons.munitions.ProjectileDamageHooks;
import rbasamoyai.createbigcannons.munitions.config.components.BallisticPropertiesComponent;
import rbasamoyai.createbigcannons.utils.CBCUtils;

public final class TerminalBallisticsCompat {
	private static final double EPSILON = 1.0E-9D;
	private static final float DURABILITY_EPSILON = 1.0E-4F;
	private static final Map<Entity, CachedImpact> IMPACT_CACHE = Collections.synchronizedMap(new WeakHashMap<>());

	private TerminalBallisticsCompat() { }

	public static CachedImpact getOrCompute(
			Entity projectile,
			Object projectileContext,
			BlockState state,
			BlockHitResult hit,
			boolean autocannonHint
	) {
		if (Config.disableRealisticBlockDamage()) return null;
		if (!(projectile instanceof AbstractCannonProjectile cannon)) return null;
		if (!(projectile instanceof AbstractCannonProjectileCompatAccessor accessor)) return null;
		if (!(projectileContext instanceof ProjectileContext context)) return null;

		float initialDurabilityMass = cannon.getProjectileMass();
		if (!Float.isFinite(initialDurabilityMass) || initialDurabilityMass <= DURABILITY_EPSILON) {
			IMPACT_CACHE.remove(projectile);
			return null;
		}

		BlockPos pos = hit.getBlockPos().immutable();
		long gameTime = projectile.level().getGameTime();
		CachedImpact cached = IMPACT_CACHE.get(projectile);
		if (cached != null
				&& cached.gameTime() == gameTime
				&& cached.blockPos().equals(pos)
				&& cached.autocannonHint() == autocannonHint
				&& Math.abs(cached.initialDurabilityMass() - initialDurabilityMass) <= DURABILITY_EPSILON) {
			return cached;
		}

		BallisticPropertiesComponent ballistics = accessor.goingballistic$invokeGetBallisticProperties();
		BlockArmorPropertiesProvider blockArmor = BlockArmorPropertiesHandler.getProperties(state);
		boolean unbreakable = context.griefState() == CBCCfgMunitions.GriefState.NO_DAMAGE
				|| state.getDestroySpeed(projectile.level(), pos) == -1
				|| !ProjectileDamageHooks.canDamageTerrain(projectile.level(), pos);

		Vec3 baseVelocity = projectile.getDeltaMovement();
		Vec3 accel = accessor.goingballistic$invokeGetForces(projectile.position(), baseVelocity);
		Vec3 curVel = baseVelocity.add(accel);
		Vec3 normal = CBCUtils.getSurfaceNormalVector(projectile.level(), hit);
		double massKg = autocannonHint
				? BallisticProjectileHelper.getProjectileMassKg(cannon, BallisticsParameterRegistry.autocannonProjectileMassFallback())
				: BallisticProjectileHelper.getProjectileMassKg(cannon);
		double toughness = blockArmor.toughness(projectile.level(), state, pos, true);
		double hardness = blockArmor.hardness(projectile.level(), state, pos, true);

		// TB keeps ownership of ricochet behavior. Disable bouncing here so the
		// shared solver is used only for penetration, deposited work, durability
		// and post-perforation velocity.
		ImpactSolution solution = BlockDamageMath.solveImpact(
				curVel,
				normal,
				massKg,
				toughness,
				hardness,
				ballistics.penetration(),
				ballistics.toughness(),
				ballistics.deflection(),
				initialDurabilityMass,
				accessor.goingballistic$invokeCanHitSurface(),
				false,
				unbreakable,
				true,
				0.0D,
				1.0D
		);

		CachedImpact result = new CachedImpact(
				gameTime,
				pos,
				autocannonHint,
				initialDurabilityMass,
				curVel,
				massKg,
				solution
		);
		IMPACT_CACHE.put(projectile, result);
		return result;
	}

	public static void clear(Entity projectile) {
		IMPACT_CACHE.remove(projectile);
	}

	public static double penetrationRatio(CachedImpact impact) {
		if (impact == null) return Double.NaN;
		double requiredWorkJ = impact.solution().requiredWorkJ();
		double impactEnergyJ = impact.solution().impactEnergyJ();
		if (requiredWorkJ <= EPSILON) return impactEnergyJ > EPSILON ? Double.POSITIVE_INFINITY : 0.0D;
		return Math.max(0.0D, impactEnergyJ / requiredWorkJ);
	}

	public static double integrityWorkFraction(CachedImpact impact) {
		double ratio = penetrationRatio(impact);
		if (!Double.isFinite(ratio)) return ratio > 0.0D ? 1.0D : 0.0D;
		return Math.max(0.0D, Math.min(1.0D, ratio));
	}

	public record CachedImpact(
			long gameTime,
			BlockPos blockPos,
			boolean autocannonHint,
			float initialDurabilityMass,
			Vec3 preImpactVelocity,
			double physicalMassKg,
			ImpactSolution solution
	) { }
}