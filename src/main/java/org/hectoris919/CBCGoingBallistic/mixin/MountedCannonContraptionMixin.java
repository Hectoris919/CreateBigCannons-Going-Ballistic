package org.hectoris919.CBCGoingBallistic.mixin;

import net.minecraft.server.level.ServerLevel;
import org.hectoris919.CBCGoingBallistic.Config;
import org.hectoris919.CBCGoingBallistic.ballistics.BallisticProjectileHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rbasamoyai.createbigcannons.cannon_control.contraption.AbstractMountedCannonContraption;
import rbasamoyai.createbigcannons.cannon_control.contraption.MountedBigCannonContraption;
import rbasamoyai.createbigcannons.cannon_control.contraption.PitchOrientedContraptionEntity;
import rbasamoyai.createbigcannons.cannons.big_cannons.material.BigCannonMaterialProperties;
import rbasamoyai.createbigcannons.munitions.big_cannon.AbstractBigCannonProjectile;

@Mixin(MountedBigCannonContraption.class)
public abstract class MountedCannonContraptionMixin {
	@Unique
	private AbstractBigCannonProjectile goingballistic$currentProjectile;

	@Inject(method = "fireShot", at = @At("HEAD"))
	private void goingballistic$beginPropellantCapture(ServerLevel level, PitchOrientedContraptionEntity entity, CallbackInfo ci) {
		BallisticProjectileHelper.beginBigCannonShot(level.registryAccess());
	}

	@Inject(method = "fireShot", at = @At("RETURN"))
	private void goingballistic$endPropellantCapture(ServerLevel level, PitchOrientedContraptionEntity entity, CallbackInfo ci) {
		BallisticProjectileHelper.endBigCannonShot();
	}

	@Inject(
			method = "fireShot",
			at = @At(
					value = "INVOKE",
					target = "Lrbasamoyai/createbigcannons/cannon_control/ControlPitchContraption;onRecoil(Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;Lcom/simibubi/create/content/contraptions/AbstractContraptionEntity;)V"
			)
	)
	private void goingballistic$finalizeRecoilTransaction(ServerLevel level, PitchOrientedContraptionEntity entity, CallbackInfo ci) {
		BallisticProjectileHelper.finalizeBigCannonRecoil();
	}

	@Redirect(
			method = "fireShot",
			at = @At(
					value = "INVOKE",
					target = "Lrbasamoyai/createbigcannons/munitions/big_cannon/AbstractBigCannonProjectile;canSquib()Z"
			)
	)
	private boolean goingballistic$getProjectileForJammingCheck(AbstractBigCannonProjectile projectile) {
		boolean canSquib = projectile.canSquib();
		this.goingballistic$currentProjectile = canSquib ? projectile : null;
		return canSquib;
	}

	@Redirect(
			method = "fireShot",
			at = @At(
					value = "INVOKE",
					target = "Lrbasamoyai/createbigcannons/cannons/big_cannons/material/BigCannonMaterialProperties;mayGetStuck(FI)Z"
			)
	)
	private boolean goingballistic$useDryBoreSquibCheck(BigCannonMaterialProperties properties, float chargesUsed, int barrelTravelled) {
		AbstractBigCannonProjectile projectile = this.goingballistic$currentProjectile;
		this.goingballistic$currentProjectile = null;

		if (Config.disableRealisticBallistics() || projectile == null) {
			return properties.mayGetStuck(chargesUsed, barrelTravelled);
		}

		return BallisticProjectileHelper.wouldCannonSquib(projectile, chargesUsed, barrelTravelled);
	}

	@Redirect(
			method = "fireShot",
			at = @At(
					value = "INVOKE",
					target = "Lrbasamoyai/createbigcannons/munitions/big_cannon/AbstractBigCannonProjectile;shoot(DDDFF)V"
			)
	)
	private void goingballistic$replaceCannonMuzzleVelocity(AbstractBigCannonProjectile projectile, double x, double y, double z, float velocity, float spread) {
		float realisticVelocity = BallisticProjectileHelper.calculateCannonLaunchVelocityBlocksPerTick(
				projectile,
				velocity,
				(AbstractMountedCannonContraption) (Object) this
		);
		projectile.shoot(x, y, z, realisticVelocity, spread);
	}
}