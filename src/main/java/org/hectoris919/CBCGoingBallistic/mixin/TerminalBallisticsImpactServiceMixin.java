package org.hectoris919.CBCGoingBallistic.mixin;

import org.hectoris919.CBCGoingBallistic.Config;
import org.hectoris919.CBCGoingBallistic.ballistics.TerminalBallisticsCompat;
import org.hectoris919.CBCGoingBallistic.ballistics.TerminalBallisticsCompat.CachedImpact;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import rbasamoyai.createbigcannons.munitions.AbstractCannonProjectile.ImpactResult;

@Pseudo
@Mixin(targets = "com.cbc_terminal_ballistics.ballistics.TBImpactService", remap = false)
public abstract class TerminalBallisticsImpactServiceMixin {

	@ModifyVariable(method = "calculate", at = @At("STORE"), name = "attack", require = 0)
	private static double goingballistic$replaceTerminalBallisticsAttack(
			double original,
			Entity projectile,
			Object projectileContext,
			BlockState state,
			BlockHitResult hit,
			boolean autocannonHint
	) {
		CachedImpact impact = TerminalBallisticsCompat.getOrCompute(projectile, projectileContext, state, hit, autocannonHint);
		return impact == null ? original : impact.solution().impactEnergyJ();
	}

	@ModifyVariable(method = "calculate", at = @At("STORE"), name = "perforationResistance", require = 0)
	private static double goingballistic$replaceTerminalBallisticsResistance(
			double original,
			Entity projectile,
			Object projectileContext,
			BlockState state,
			BlockHitResult hit,
			boolean autocannonHint
	) {
		CachedImpact impact = TerminalBallisticsCompat.getOrCompute(projectile, projectileContext, state, hit, autocannonHint);
		return impact == null ? original : Math.max(impact.solution().requiredWorkJ(), 1.0E-9D);
	}

	@ModifyVariable(method = "calculate", at = @At("STORE"), name = "penetrationRatio", require = 0)
	private static double goingballistic$replaceTerminalBallisticsPenetrationRatio(
			double original,
			Entity projectile,
			Object projectileContext,
			BlockState state,
			BlockHitResult hit,
			boolean autocannonHint
	) {
		CachedImpact impact = TerminalBallisticsCompat.getOrCompute(projectile, projectileContext, state, hit, autocannonHint);
		if (impact == null) return original;
		double ratio = TerminalBallisticsCompat.penetrationRatio(impact);
		return Double.isNaN(ratio) ? original : ratio;
	}

	@ModifyVariable(method = "calculate", at = @At("STORE"), name = "impactSeverity", require = 0)
	private static double goingballistic$replaceTerminalBallisticsImpactSeverity(
			double original,
			Entity projectile,
			Object projectileContext,
			BlockState state,
			BlockHitResult hit,
			boolean autocannonHint
	) {
		CachedImpact impact = TerminalBallisticsCompat.getOrCompute(projectile, projectileContext, state, hit, autocannonHint);
		return impact == null ? original : TerminalBallisticsCompat.integrityWorkFraction(impact);
	}

	@Inject(method = "calculate", at = @At("RETURN"), require = 0)
	private static void goingballistic$applyTerminalBallisticsPostImpactPhysics(
			Entity projectile,
			Object projectileContext,
			BlockState state,
			BlockHitResult hit,
			boolean autocannonHint,
			CallbackInfoReturnable<Object> cir
	) {
		CachedImpact impact = TerminalBallisticsCompat.peek(projectile, hit);
		try {
			if (Config.disableRealisticBlockDamage() || impact == null) return;
			Object returned = cir.getReturnValue();
			if (returned instanceof ImpactResult result
					&& result.kinematics() == ImpactResult.KinematicOutcome.PENETRATE) {
				TerminalBallisticsCompat.applyPenetrationKinematics(projectile, impact);
			}
		} finally {
			TerminalBallisticsCompat.clear(projectile);
		}
	}
}