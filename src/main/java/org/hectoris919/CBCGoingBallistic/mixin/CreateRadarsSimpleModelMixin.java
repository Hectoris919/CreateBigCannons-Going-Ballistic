package org.hectoris919.CBCGoingBallistic.mixin;

import org.hectoris919.CBCGoingBallistic.compat.RadarCbcModelMarker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Pseudo
@Mixin(targets = "com.happysg.radar.targeting.ProjectileModel", remap = false)
public interface CreateRadarsSimpleModelMixin {
    @ModifyArgs(
            method = "simple(DDDZ)Lcom/happysg/radar/targeting/ProjectileModel;",
            at = @At(value = "INVOKE",
                    target = "Lcom/happysg/radar/targeting/ProjectileModel$SimpleProjectileModel;<init>(DDDZZD)V",
                    remap = false),
            remap = false,
            require = 0
    )
    private static void goingballistic$preserveCbcIntegration(Args args) {
        Double density = RadarCbcModelMarker.consume();
        if (density == null) return;
        args.set(4, true); // cbcPhysics: integrate position with half-step acceleration
        args.set(5, density); // dimension drag; Radar adds loaded-fluid drag itself
    }
}