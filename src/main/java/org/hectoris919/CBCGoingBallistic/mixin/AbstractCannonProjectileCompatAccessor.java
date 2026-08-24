package org.hectoris919.CBCGoingBallistic.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import net.minecraft.world.phys.Vec3;
import rbasamoyai.createbigcannons.munitions.AbstractCannonProjectile;
import rbasamoyai.createbigcannons.munitions.config.components.BallisticPropertiesComponent;

@Mixin(AbstractCannonProjectile.class)
public interface AbstractCannonProjectileCompatAccessor {
	@Invoker("getForces")
	Vec3 goingballistic$invokeGetForces(Vec3 position, Vec3 velocity);

	@Invoker("canHitSurface")
	boolean goingballistic$invokeCanHitSurface();

	@Invoker("getBallisticProperties")
	BallisticPropertiesComponent goingballistic$invokeGetBallisticProperties();
}