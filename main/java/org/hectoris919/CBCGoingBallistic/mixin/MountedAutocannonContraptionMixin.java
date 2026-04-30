package org.hectoris919.CBCGoingBallistic.mixin;

import org.hectoris919.CBCGoingBallistic.ballistics.BallisticProjectileHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import rbasamoyai.createbigcannons.cannon_control.contraption.AbstractMountedCannonContraption;
import rbasamoyai.createbigcannons.cannon_control.contraption.MountedAutocannonContraption;
import rbasamoyai.createbigcannons.munitions.autocannon.AbstractAutocannonProjectile;

@Mixin(MountedAutocannonContraption.class)
public abstract class MountedAutocannonContraptionMixin {
	@Redirect(
			method = "fireShot",
			at = @At(
					value = "INVOKE",
					target = "Lrbasamoyai/createbigcannons/munitions/autocannon/AbstractAutocannonProjectile;shoot(DDDFF)V"
			)
	)
	private void goingballistic$replaceAutocannonMuzzleVelocity(AbstractAutocannonProjectile projectile, double x, double y, double z, float velocity, float spread) {
		float realisticVelocity = BallisticProjectileHelper.calculateAutocannonLaunchVelocityBlocksPerTick(
				projectile,
				velocity,
				(AbstractMountedCannonContraption) (Object) this
		);
		projectile.shoot(x, y, z, realisticVelocity, spread);
	}
}
