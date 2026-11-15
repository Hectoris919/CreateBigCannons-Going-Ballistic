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
import net.minecraft.world.phys.Vec3;
import rbasamoyai.createbigcannons.munitions.AbstractCannonProjectile.ImpactResult;

@Pseudo
@Mixin(targets = "com.cbc_terminal_ballistics.ballistics.TBImpactService", remap = false)
public abstract class TerminalBallisticsImpactServiceMixin {
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
		if (Double.isNaN(ratio)) return original;
		return Math.max(0.0D, Math.min(ratio, 1.0E6D));
	}

	@Inject(method = "calculate", at = @At("RETURN"), require = 0)
	private static void goingballistic$clearTerminalBallisticsCache(
			Entity projectile,
			Object projectileContext,
			BlockState state,
			BlockHitResult hit,
			boolean autocannonHint,
			CallbackInfoReturnable<Object> cir
	) {
		CachedImpact impact = TerminalBallisticsCompat.peek(projectile, hit);
		try {
			if (Config.disableRealisticBlockDamage() || impact == null || projectile.isRemoved()) return;
			if (!(cir.getReturnValue() instanceof ImpactResult result)
					|| result.kinematics() != ImpactResult.KinematicOutcome.PENETRATE) return;

			Vec3 velocity = projectile.getDeltaMovement();
			double currentSpeed = velocity.length();
			double gbSpeed = impact.solution().postImpactVelocity().length();
			if (currentSpeed > 1.0E-12D && Double.isFinite(gbSpeed)
					&& gbSpeed < currentSpeed) {
				projectile.setDeltaMovement(velocity.scale(gbSpeed / currentSpeed));
			}
		} finally {
			TerminalBallisticsCompat.clear(projectile);
		}
	}
}