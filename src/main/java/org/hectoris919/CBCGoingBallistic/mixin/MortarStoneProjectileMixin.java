package org.hectoris919.CBCGoingBallistic.mixin;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import org.hectoris919.CBCGoingBallistic.Config;
import org.hectoris919.CBCGoingBallistic.data.ProjectileMassProperties;
import org.hectoris919.CBCGoingBallistic.data.ProjectileMassRegistry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rbasamoyai.createbigcannons.munitions.big_cannon.mortar_stone.MortarStoneProjectile;

@Mixin(MortarStoneProjectile.class)
public abstract class MortarStoneProjectileMixin {
	@Shadow
	private boolean tooManyCharges;

	@Inject(method = "setChargePower", at = @At("HEAD"), cancellable = true)
	private void goingballistic$useChargeEquivalentDisintegrationLimit(float power, CallbackInfo ci) {
		if (Config.disableRealisticBallistics()) return;

		Entity self = (Entity) (Object) this;
		ResourceLocation projectileId = BuiltInRegistries.ENTITY_TYPE.getKey(self.getType());
		ProjectileMassProperties properties = ProjectileMassRegistry.getProperties(projectileId).orElse(null);
		if (properties == null || properties.maxSafeChargeEquivalentsOptional().isEmpty()) return;

		double chargeEquivalents = power / 2.0D;
		this.tooManyCharges = chargeEquivalents > properties.maxSafeChargeEquivalentsOptional().getAsDouble();
		ci.cancel();
	}
}
