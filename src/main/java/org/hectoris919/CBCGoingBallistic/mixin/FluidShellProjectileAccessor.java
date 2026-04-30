package org.hectoris919.CBCGoingBallistic.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import rbasamoyai.createbigcannons.munitions.big_cannon.fluid_shell.EndFluidStack;
import rbasamoyai.createbigcannons.munitions.big_cannon.fluid_shell.FluidShellProjectile;

@Mixin(FluidShellProjectile.class)
public interface FluidShellProjectileAccessor {
	@Accessor("fluidStack")
	EndFluidStack goingballistic$getFluidStack();
}
