package org.hectoris919.CBCGoingBallistic.mixin;

import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.hectoris919.CBCGoingBallistic.ballistics.SableImpulseMath;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rbasamoyai.createbigcannons.munitions.AbstractCannonProjectile;
import rbasamoyai.createbigcannons.munitions.ProjectileContext;

@Pseudo
@Mixin(targets = "rbasamoyai.createbigcannons.compat.sable.SableCompat", remap = false)
public abstract class SableCompatMixin {
	@Unique
	private static final ThreadLocal<Double> goingballistic$recoilScale = new ThreadLocal<>();

	@Unique
	private static final ThreadLocal<Double> goingballistic$impactImpulse = new ThreadLocal<>();

	@Inject(method = "recoilCannon", at = @At("HEAD"), require = 1)
	private static void goingballistic$preparePhysicalRecoil(Level level, Vec3 pos, Vec3 recoil, CallbackInfo ci) {
		SableImpulseMath.RecoilImpulseOverride override = SableImpulseMath.consumeRecoilOverride();
		if (!override.calculated()) {
			goingballistic$recoilScale.remove();
			return;
		}

		double recoilLength = recoil == null ? 0.0D : recoil.length();
		double replacementScale = recoilLength <= 1.0E-12D
				? 0.0D
				: override.impulseMagnitude() / recoilLength;

		goingballistic$recoilScale.set(replacementScale);
	}

	@ModifyArg(
			method = "recoilCannon",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/world/phys/Vec3;scale(D)Lnet/minecraft/world/phys/Vec3;", remap = true),
			index = 0,
			require = 1
	)
	private static double goingballistic$replaceRecoilMagnitude(double originalScale) {
		Double replacement = goingballistic$recoilScale.get();
		return replacement == null ? originalScale : replacement;
	}

	@Inject(method = "recoilCannon", at = @At("RETURN"), require = 1)
	private static void goingballistic$clearPhysicalRecoil(Level level, Vec3 pos, Vec3 recoil, CallbackInfo ci) {
		goingballistic$recoilScale.remove();
	}

	@Inject(method = "impactContraption", at = @At("HEAD"), require = 1)
	private static void goingballistic$preparePhysicalImpact(
			Level level,
			HitResult hitResult,
			AbstractCannonProjectile.ImpactResult impactResult,
			ProjectileContext projectileContext,
			CallbackInfo ci
	) {
		double physicalImpulse = projectileContext == null || projectileContext.projectile == null
				? 0.0D
				: SableImpulseMath.consumeImpactImpulse(projectileContext.projectile);
		if (physicalImpulse <= 0.0D) {
			goingballistic$impactImpulse.remove();
			return;
		}

		// See recoil handling above: CBC's default 40x multiplier calibrates its own
		// velocity-based impactPower values, not a physical momentum impulse.
		goingballistic$impactImpulse.set(physicalImpulse);
	}

	@ModifyArg(
			method = "impactContraption",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/world/phys/Vec3;scale(D)Lnet/minecraft/world/phys/Vec3;", remap = true),
			index = 0,
			require = 1,
			allow = 2
	)
	private static double goingballistic$replaceImpactMagnitude(double originalScale) {
		Double replacement = goingballistic$impactImpulse.get();
		return replacement == null ? originalScale : replacement;
	}

	@Inject(method = "impactContraption", at = @At("RETURN"), require = 1)
	private static void goingballistic$clearPhysicalImpact(
			Level level,
			HitResult hitResult,
			AbstractCannonProjectile.ImpactResult impactResult,
			ProjectileContext projectileContext,
			CallbackInfo ci
	) {
		goingballistic$impactImpulse.remove();
	}
}