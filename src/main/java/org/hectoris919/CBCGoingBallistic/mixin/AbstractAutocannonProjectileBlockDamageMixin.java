package org.hectoris919.CBCGoingBallistic.mixin;

import org.hectoris919.CBCGoingBallistic.Config;
import org.hectoris919.CBCGoingBallistic.ballistics.BallisticProjectileHelper;
import org.hectoris919.CBCGoingBallistic.ballistics.BlockDamageMath;
import org.hectoris919.CBCGoingBallistic.ballistics.SableImpulseMath;
import org.hectoris919.CBCGoingBallistic.data.BallisticsParameterRegistry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import rbasamoyai.createbigcannons.CreateBigCannons;
import rbasamoyai.createbigcannons.block_armor_properties.BlockArmorPropertiesHandler;
import rbasamoyai.createbigcannons.block_armor_properties.BlockArmorPropertiesProvider;
import rbasamoyai.createbigcannons.config.CBCCfgMunitions;
import rbasamoyai.createbigcannons.config.CBCConfigs;
import rbasamoyai.createbigcannons.munitions.AbstractCannonProjectile;
import rbasamoyai.createbigcannons.munitions.ProjectileContext;
import rbasamoyai.createbigcannons.munitions.ProjectileDamageHooks;
import rbasamoyai.createbigcannons.munitions.autocannon.AbstractAutocannonProjectile;
import rbasamoyai.createbigcannons.munitions.config.components.BallisticPropertiesComponent;
import rbasamoyai.createbigcannons.network.ClientboundPlayBlockHitEffectPacket;
import rbasamoyai.createbigcannons.utils.CBCUtils;

@Mixin(AbstractAutocannonProjectile.class)
public abstract class AbstractAutocannonProjectileBlockDamageMixin extends AbstractCannonProjectile {
	protected AbstractAutocannonProjectileBlockDamageMixin(EntityType<? extends AbstractCannonProjectile> type, Level level) {
		super(type, level);
	}

	@Inject(method = "calculateBlockPenetration", at = @At("HEAD"), cancellable = true)
	private void goingballistic$replaceAutocannonBlockPenetration(ProjectileContext projectileContext, BlockState state, BlockHitResult blockHitResult, CallbackInfoReturnable<ImpactResult> cir) {
		if (Config.disableRealisticBlockDamage()) return;

		BlockPos pos = blockHitResult.getBlockPos();
		Vec3 hitLoc = blockHitResult.getLocation();
		BallisticPropertiesComponent ballistics = this.getBallisticProperties();
		BlockArmorPropertiesProvider blockArmor = BlockArmorPropertiesHandler.getProperties(state);
		boolean unbreakable = projectileContext.griefState() == CBCCfgMunitions.GriefState.NO_DAMAGE
				|| state.getDestroySpeed(this.level(), pos) == -1
				|| !ProjectileDamageHooks.canDamageTerrain(this.level(), pos);

		Vec3 accel = this.getForces(this.position(), this.getDeltaMovement());
		Vec3 curVel = this.getDeltaMovement().add(accel);
		Vec3 normal = CBCUtils.getSurfaceNormalVector(this.level(), blockHitResult);
		double incidence = BlockDamageMath.incidence(curVel, normal);
		double velocityMps = BlockDamageMath.velocityBlocksPerTickToMetersPerSecond(curVel.length());
		double massKg = BallisticProjectileHelper.getProjectileMassKg(this, BallisticsParameterRegistry.autocannonProjectileMassFallback());

		double impactEnergyJ = BlockDamageMath.impactEnergyJ(massKg, velocityMps);
		double toughness = blockArmor.toughness(this.level(), state, pos, true);
		double requiredWorkJ = BlockDamageMath.perforationWorkJ(toughness, incidence);
		double hardnessPenalty = Math.max(blockArmor.hardness(this.level(), state, pos, true) - ballistics.penetration(), 0.0D);

		double projectileDeflection = ballistics.deflection();
		double baseChance = CBCConfigs.server().munitions.baseProjectileBounceChance.getF();
		double bounceChance = projectileDeflection < 1.0E-2D || incidence > projectileDeflection
				? 0.0D
				: Math.max(baseChance, 1.0D - incidence / projectileDeflection);
		boolean surfaceImpact = this.lastPenetratedBlock.isAir();
		boolean canBounce = CBCConfigs.server().munitions.projectilesCanBounce.get();

		ImpactResult.KinematicOutcome outcome;
		if (surfaceImpact && canBounce && this.level().getRandom().nextDouble() < bounceChance) {
			outcome = ImpactResult.KinematicOutcome.BOUNCE;
		} else {
			outcome = ImpactResult.KinematicOutcome.STOP;
		}

		boolean shatter = surfaceImpact && outcome != ImpactResult.KinematicOutcome.BOUNCE && hardnessPenalty > ballistics.toughness();

		float initialDurabilityMass = this.getProjectileMass();
		if (outcome == ImpactResult.KinematicOutcome.STOP) this.setProjectileMass(0.0F);

		double depositedEnergyJ = outcome == ImpactResult.KinematicOutcome.BOUNCE
				? BlockDamageMath.bounceDepositedEnergyJ(impactEnergyJ, incidence)
				: impactEnergyJ;
		double structuralCoupling = BlockDamageMath.autocannonStructuralDamageCoupling(massKg);
		double structurallyCoupledEnergyJ = depositedEnergyJ * structuralCoupling;
		double damagePoints = unbreakable
				? 0.0D
				: BlockDamageMath.partialDamagePoints(structurallyCoupledEnergyJ, requiredWorkJ, toughness);

		Vec3 postImpactVelocity = outcome == ImpactResult.KinematicOutcome.BOUNCE
				? SableImpulseMath.bouncedVelocity(curVel, normal)
				: Vec3.ZERO;
		SableImpulseMath.captureImpactImpulse(this, massKg, curVel, postImpactVelocity);

		if (!this.level().isClientSide) {
			boolean bounced = outcome == ImpactResult.KinematicOutcome.BOUNCE;
			Vec3 effectNormal;
			if (bounced) {
				double elasticity = 1.7D;
				effectNormal = curVel.subtract(normal.scale(normal.dot(curVel) * elasticity));
			} else {
				effectNormal = curVel.reverse();
			}
			for (BlockState containedState : blockArmor.containedBlockStates(this.level(), state, pos.immutable(), true)) {
				projectileContext.addPlayedEffect(new ClientboundPlayBlockHitEffectPacket(containedState, this.getType(), bounced, true, hitLoc.x, hitLoc.y, hitLoc.z, (float) effectNormal.x, (float) effectNormal.y, (float) effectNormal.z));
			}
			if (damagePoints > 1.0E-6D) {
				CreateBigCannons.BLOCK_DAMAGE.damageBlock(pos.immutable(), (float) damagePoints, state, this.level());
			}
		}

		this.massLost = ballistics.durabilityMass() < 1.0E-4D
				? 0.0F
				: ((initialDurabilityMass - this.getProjectileMass()) / ballistics.durabilityMass());
		BlockDamageMath.logAutocannonImpact(
				this,
				massKg,
				velocityMps,
				incidence,
				impactEnergyJ,
				requiredWorkJ,
				depositedEnergyJ,
				structuralCoupling,
				structurallyCoupledEnergyJ,
				damagePoints,
				hardnessPenalty,
				outcome.name()
		);

		this.onImpact(blockHitResult, new ImpactResult(outcome, shatter), projectileContext);
		cir.setReturnValue(new ImpactResult(outcome, !this.level().isClientSide && (shatter || outcome != ImpactResult.KinematicOutcome.BOUNCE)));
	}
}