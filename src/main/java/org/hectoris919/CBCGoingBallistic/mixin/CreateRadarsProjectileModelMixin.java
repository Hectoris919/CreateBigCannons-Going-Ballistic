package org.hectoris919.CBCGoingBallistic.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.server.level.ServerLevel;
import org.hectoris919.CBCGoingBallistic.Config;
import org.hectoris919.CBCGoingBallistic.compat.RadarCbcModelMarker;
import org.hectoris919.CBCGoingBallistic.ballistics.BallisticProjectileHelper;
import org.hectoris919.CBCGoingBallistic.ballistics.ProjectilePhysicsMath;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;
import rbasamoyai.createbigcannons.cannon_control.contraption.AbstractMountedCannonContraption;

@Pseudo
@Mixin(targets = "com.happysg.radar.block.behavior.networks.WeaponFiringControl", remap = false)
public abstract class CreateRadarsProjectileModelMixin {
    @ModifyArgs(
            method = "resolveProjectileState(Lcom/happysg/radar/compat/cbc/CannonMountContext;Lrbasamoyai/createbigcannons/cannon_control/contraption/AbstractMountedCannonContraption;Lnet/minecraft/server/level/ServerLevel;)Lcom/happysg/radar/block/behavior/networks/WeaponFiringControl$ResolvedProjectileState;",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/happysg/radar/targeting/ProjectileModel;simple(DDDZ)Lcom/happysg/radar/targeting/ProjectileModel;",
                    remap = false
            ),
            remap = false,
            require = 0
    )
    private void goingballistic$usePhysicalAutocannonTrajectory(
            Args args,
            @Local(argsOnly = true) AbstractMountedCannonContraption cannon,
            @Local(argsOnly = true) ServerLevel level
    ) {
        if (Config.disableRealisticBallistics() || !(cannon instanceof rbasamoyai.createbigcannons.cannon_control.contraption.MountedAutocannonContraption)) return;

        float defaultSpeed = ((Number) args.get(0)).floatValue();
        BallisticProjectileHelper.RadarShotEstimate estimate =
                BallisticProjectileHelper.estimateAutocannonForRadar(cannon, level, defaultSpeed);
        if (estimate == null) return;
        if (estimate.muzzleSpeedBlocksPerTick() > 0.0F) args.set(0, (double) estimate.muzzleSpeedBlocksPerTick());

        if (!Config.disableRealisticProjectileGravity()) {
            args.set(1, ProjectilePhysicsMath.earthGravityBlocksPerTickSquared()
                    * rbasamoyai.createbigcannons.munitions.config.DimensionMunitionPropertiesHandler
                    .getProperties(level).gravityMultiplier());
        }
        if (!Config.disableRealisticProjectileDrag()) {
            args.set(2, estimate.quadraticDragCoefficient());
            args.set(3, true);
        }

        RadarCbcModelMarker.markNextAutocannon(rbasamoyai.createbigcannons.munitions.config.DimensionMunitionPropertiesHandler.getProperties(level).dragMultiplier());
    }

    @ModifyArgs(
            method = "resolveProjectileState(Lcom/happysg/radar/compat/cbc/CannonMountContext;Lrbasamoyai/createbigcannons/cannon_control/contraption/AbstractMountedCannonContraption;Lnet/minecraft/server/level/ServerLevel;)Lcom/happysg/radar/block/behavior/networks/WeaponFiringControl$ResolvedProjectileState;",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/happysg/radar/targeting/ProjectileModel;cbc(DDDDZ)Lcom/happysg/radar/targeting/ProjectileModel;",
                    ordinal = 1,
                    remap = false
            ),
            remap = false,
            require = 0
    )
    private void goingballistic$usePhysicalTrajectory(
            Args args,
            @Local(argsOnly = true) AbstractMountedCannonContraption cannon,
            @Local(argsOnly = true) ServerLevel level
    ) {
        if (Config.disableRealisticBallistics()) return;

        float defaultSpeed = ((Number) args.get(0)).floatValue();
        BallisticProjectileHelper.RadarShotEstimate estimated =
                BallisticProjectileHelper.estimateBigCannonForRadar(cannon, level, defaultSpeed);
        if (estimated != null && estimated.muzzleSpeedBlocksPerTick() > 0.0F) {
            args.set(0, (double) estimated.muzzleSpeedBlocksPerTick());
        }
        if (!Config.disableRealisticProjectileGravity()) {
            args.set(1, ProjectilePhysicsMath.earthGravityBlocksPerTickSquared()
                    * rbasamoyai.createbigcannons.munitions.config.DimensionMunitionPropertiesHandler
                            .getProperties(level).gravityMultiplier());
        }
        if (!Config.disableRealisticProjectileDrag() && estimated != null) {
            args.set(2, estimated.quadraticDragCoefficient());
            args.set(4, true);
        }
    }
}