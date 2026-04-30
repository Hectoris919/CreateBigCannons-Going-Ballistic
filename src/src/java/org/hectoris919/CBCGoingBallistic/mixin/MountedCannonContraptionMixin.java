package org.hectoris919.CBCGoingBallistic.mixin;

import org.hectoris919.CBCGoingBallistic.ballistics.BallisticProjectileHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import rbasamoyai.createbigcannons.cannon_control.contraption.AbstractMountedCannonContraption;
import rbasamoyai.createbigcannons.cannon_control.contraption.MountedBigCannonContraption;
import rbasamoyai.createbigcannons.cannons.big_cannons.material.BigCannonMaterialProperties;
import rbasamoyai.createbigcannons.munitions.big_cannon.AbstractBigCannonProjectile;

@Mixin(MountedBigCannonContraption.class)
public abstract class MountedCannonContraptionMixin {
	@Unique
	private AbstractBigCannonProjectile goingballistic$currentProjectile;

	@Redirect(
			method = "fireShot",
			at = @At(
					value = "INVOKE",
					target = "Lrbasamoyai/createbigcannons/munitions/big_cannon/AbstractBigCannonProjectile;canSquib()Z"
			)
	)
	private boolean goingballistic$getProjectileForJammingCheck(AbstractBigCannonProjectile projectile) {
		this.goingballistic$currentProjectile = projectile;
		return projectile.canSquib();
	}

	@Redirect(
			method = "fireShot",
			at = @At(
					value = "INVOKE",
					target = "Lrbasamoyai/createbigcannons/cannons/big_cannons/material/BigCannonMaterialProperties;mayGetStuck(FI)Z"
			)
	)
	private boolean goingballistic$useVelocityForJammingCheck(BigCannonMaterialProperties properties, float chargesUsed, int barrelTravelled) {
		AbstractBigCannonProjectile projectile = this.goingballistic$currentProjectile;
		this.goingballistic$currentProjectile = null;

		float effectiveVelocity = BallisticProjectileHelper.calculateCannonJammingVelocityBlocksPerTick(
				projectile,
				chargesUsed,
				barrelTravelled
		);
		return properties.mayGetStuck(effectiveVelocity, barrelTravelled);
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
