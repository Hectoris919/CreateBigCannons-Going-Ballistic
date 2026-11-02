package org.hectoris919.CBCGoingBallistic.mixin;

import org.hectoris919.CBCGoingBallistic.Config;
import org.hectoris919.CBCGoingBallistic.ballistics.BallisticProjectileHelper;
import org.hectoris919.CBCGoingBallistic.ballistics.ProjectilePhysicsMath;
import org.hectoris919.CBCGoingBallistic.data.BallisticsParameterRegistry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import rbasamoyai.createbigcannons.munitions.AbstractCannonProjectile;
import rbasamoyai.createbigcannons.munitions.config.DimensionMunitionPropertiesHandler;
import rbasamoyai.createbigcannons.munitions.config.FluidDragHandler;
import net.minecraft.world.level.material.FluidState;

@Mixin(AbstractCannonProjectile.class)
public abstract class AbstractCannonProjectilePhysicsMixin {

	@Inject(method = "getDefaultGravity", at = @At("HEAD"), cancellable = true)
	private void goingballistic$useEarthGravity(CallbackInfoReturnable<Double> cir) {
		if (Config.disableRealisticBallistics() || Config.disableRealisticProjectileGravity()) {
			return;
		}

		AbstractCannonProjectile projectile = (AbstractCannonProjectile) (Object) this;
		if (projectile.isNoGravity()) {
			cir.setReturnValue(0.0D);
			return;
		}

		double dimensionGravityMultiplier = DimensionMunitionPropertiesHandler
				.getProperties(projectile.level())
				.gravityMultiplier();

		cir.setReturnValue(ProjectilePhysicsMath.earthGravityBlocksPerTickSquared() * dimensionGravityMultiplier);
	}

	@Inject(method = "getDragForce", at = @At("HEAD"), cancellable = true)
	private void goingballistic$useCalculatedQuadraticDrag(CallbackInfoReturnable<Double> cir) {
		if (Config.disableRealisticBallistics() || Config.disableRealisticProjectileDrag()) {
			return;
		}

		AbstractCannonProjectile projectile = (AbstractCannonProjectile) (Object) this;

		double velocityBlocksPerTick = projectile.getDeltaMovement().length();
		if (!Double.isFinite(velocityBlocksPerTick) || velocityBlocksPerTick <= 0.0D) {
			cir.setReturnValue(0.0D);
			return;
		}

		double massKg = BallisticProjectileHelper.getProjectileMassKg(projectile);
		double dimensionDragMultiplier = DimensionMunitionPropertiesHandler
				.getProperties(projectile.level())
				.dragMultiplier();

		FluidState fluid = projectile.level().getFluidState(projectile.blockPosition());
		double fluidDrag = fluid.isEmpty() ? 0.0D : FluidDragHandler.getFluidDrag(fluid);
		double densityMultiplier = Math.max(0.0D, dimensionDragMultiplier + fluidDrag);
		double airDensityKgPerM3 = BallisticsParameterRegistry.airDensity() * densityMultiplier;

		double dragAcceleration = ProjectilePhysicsMath.dragAccelerationBlocksPerTickSquared(
				projectile,
				velocityBlocksPerTick,
				massKg,
				BallisticsParameterRegistry.projectileDragCoefficient(),
				airDensityKgPerM3
		);

		cir.setReturnValue(Math.min(dragAcceleration, velocityBlocksPerTick));
	}
}