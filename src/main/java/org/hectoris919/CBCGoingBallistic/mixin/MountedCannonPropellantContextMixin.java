package org.hectoris919.CBCGoingBallistic.mixin;

import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
import org.hectoris919.CBCGoingBallistic.ballistics.BallisticProjectileHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import rbasamoyai.createbigcannons.munitions.big_cannon.propellant.BigCannonPropellantBlock;

@Mixin(targets = "rbasamoyai.createbigcannons.cannon_control.contraption.MountedBigCannonContraption$PropellantContext")
public abstract class MountedCannonPropellantContextMixin {
	@Redirect(
			method = "addPropellant",
			at = @At(
					value = "INVOKE",
					target = "Lrbasamoyai/createbigcannons/munitions/big_cannon/propellant/BigCannonPropellantBlock;getChargePower(Lnet/minecraft/world/level/levelgen/structure/templatesystem/StructureTemplate$StructureBlockInfo;)F"
			)
	)
	private float goingballistic$capturePhysicalPropellant(BigCannonPropellantBlock propellant, StructureBlockInfo info) {
		float chargePower = propellant.getChargePower(info);
		BallisticProjectileHelper.captureBigCannonPropellant(propellant, info, chargePower);
		return chargePower;
	}
}