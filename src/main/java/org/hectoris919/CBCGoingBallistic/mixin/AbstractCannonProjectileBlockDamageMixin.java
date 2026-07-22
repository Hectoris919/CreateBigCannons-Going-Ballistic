package org.hectoris919.CBCGoingBallistic.mixin;

import org.hectoris919.CBCGoingBallistic.Config;
import org.hectoris919.CBCGoingBallistic.ballistics.BallisticProjectileHelper;
import org.hectoris919.CBCGoingBallistic.ballistics.BlockDamageMath;
import org.hectoris919.CBCGoingBallistic.ballistics.SableImpulseMath;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import rbasamoyai.createbigcannons.CreateBigCannons;
import rbasamoyai.createbigcannons.block_armor_properties.BlockArmorPropertiesHandler;
import rbasamoyai.createbigcannons.block_armor_properties.BlockArmorPropertiesProvider;
import rbasamoyai.createbigcannons.config.CBCCfgMunitions;
import rbasamoyai.createbigcannons.config.CBCConfigs;
import rbasamoyai.createbigcannons.munitions.AbstractCannonProjectile;
import rbasamoyai.createbigcannons.munitions.ImpactExplosion;
import rbasamoyai.createbigcannons.munitions.ProjectileContext;
import rbasamoyai.createbigcannons.munitions.ProjectileDamageHooks;
import rbasamoyai.createbigcannons.munitions.big_cannon.AbstractBigCannonProjectile;
import rbasamoyai.createbigcannons.munitions.config.components.BallisticPropertiesComponent;
import rbasamoyai.createbigcannons.network.ClientboundPlayBlockHitEffectPacket;
import rbasamoyai.createbigcannons.utils.CBCUtils;

@Mixin(AbstractBigCannonProjectile.class)
public abstract class AbstractCannonProjectileBlockDamageMixin extends AbstractCannonProjectile {
	protected AbstractCannonProjectileBlockDamageMixin(EntityType<? extends AbstractCannonProjectile> type, Level level) {
		super(type, level);
	}

	@Inject(method = "calculateBlockPenetration", at = @At("HEAD"), cancellable = true)
	private void goingballistic$replaceBigCannonBlockPenetration(ProjectileContext projectileContext, BlockState state, BlockHitResult blockHitResult, CallbackInfoReturnable<ImpactResult> cir) {
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
		double massKg = BallisticProjectileHelper.getProjectileMassKg(this);
		double impactEnergyJ = BlockDamageMath.impactEnergyJ(massKg, velocityMps);

		double toughness = blockArmor.toughness(this.level(), state, pos, true);
		double requiredWorkJ = BlockDamageMath.perforationWorkJ(toughness, incidence);
		double damageScore = BlockDamageMath.perforationScore(impactEnergyJ, incidence);
		double hardnessPenalty = blockArmor.hardness(this.level(), state, pos, true) - ballistics.penetration();
		double bounceBonus = Math.max(1.0D - hardnessPenalty, 0.0D);
		double projectileDeflection = ballistics.deflection();
		double baseChance = CBCConfigs.server().munitions.baseProjectileBounceChance.getF();
		double bounceChance = projectileDeflection < 1.0E-2D || incidence > projectileDeflection
				? 0.0D
				: Math.max(baseChance, 1.0D - incidence / projectileDeflection) * bounceBonus;
		boolean surfaceImpact = this.canHitSurface();
		boolean canBounce = CBCConfigs.server().munitions.projectilesCanBounce.get();
		boolean hasPerforationEnergy = !unbreakable && impactEnergyJ >= requiredWorkJ;

		ImpactResult.KinematicOutcome outcome;
		if (surfaceImpact && canBounce && this.level().getRandom().nextDouble() < bounceChance) {
			outcome = ImpactResult.KinematicOutcome.BOUNCE;
		} else if (hasPerforationEnergy && !this.level().isClientSide) {
			outcome = ImpactResult.KinematicOutcome.PENETRATE;
		} else {
			outcome = ImpactResult.KinematicOutcome.STOP;
		}

		boolean blockBroken = hasPerforationEnergy && outcome != ImpactResult.KinematicOutcome.BOUNCE;
		boolean shatter = surfaceImpact && outcome != ImpactResult.KinematicOutcome.BOUNCE && hardnessPenalty > ballistics.toughness();
		float initialDurabilityMass = this.getProjectileMass();
		float durabilityPenalty = damageScore < 1.0E-4D
				? initialDurabilityMass
				: (float) (((Math.max(0.0D, hardnessPenalty) + 1.0D) * toughness) / damageScore);

		double depositedEnergyJ;
		if (outcome == ImpactResult.KinematicOutcome.BOUNCE) {
			depositedEnergyJ = BlockDamageMath.bounceDepositedEnergyJ(impactEnergyJ, incidence);
		} else if (blockBroken) {
			depositedEnergyJ = requiredWorkJ;
		} else {
			depositedEnergyJ = impactEnergyJ;
		}
		double partialDamagePoints = blockBroken || unbreakable
				? 0.0D
				: BlockDamageMath.partialDamagePoints(depositedEnergyJ, requiredWorkJ, toughness);

		Vec3 postImpactVelocity = switch (outcome) {
			case PENETRATE -> BlockDamageMath.velocityAfterEnergyLoss(curVel, massKg, requiredWorkJ);
			case BOUNCE -> SableImpulseMath.bouncedVelocity(curVel, normal);
			case STOP -> Vec3.ZERO;
		};
		SableImpulseMath.captureImpactImpulse(this, massKg, curVel, postImpactVelocity);

		state.onProjectileHit(this.level(), state, blockHitResult, this);

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
			if (partialDamagePoints > 1.0E-6D) {
				CreateBigCannons.BLOCK_DAMAGE.damageBlock(pos.immutable(), (float) partialDamagePoints, state, this.level());
			}
		}

		BlockDamageMath.logCannonImpact(
				this,
				massKg,
				velocityMps,
				incidence,
				impactEnergyJ,
				requiredWorkJ,
				depositedEnergyJ,
				damageScore,
				partialDamagePoints,
				toughness,
				outcome.name()
		);

		if (blockBroken) {
			this.setDeltaMovement(postImpactVelocity);
			this.setProjectileMass(Math.max(initialDurabilityMass - durabilityPenalty, 0.0F));
			this.level().setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL_IMMEDIATE);
			if (surfaceImpact) {
				float workRatio = impactEnergyJ <= 1.0E-4D
						? 1.0F
						: (float) (requiredWorkJ / impactEnergyJ);
				float overPenetrationPower = workRatio < 0.15F
						? 2.0F - 2.0F * workRatio
						: 0.0F;
				if (overPenetrationPower > 0.0F && outcome == ImpactResult.KinematicOutcome.PENETRATE) {
					projectileContext.queueExplosion(pos, overPenetrationPower);
				}
			}
		} else {
			if (outcome == ImpactResult.KinematicOutcome.STOP) {
				this.setProjectileMass(0.0F);
				if (!this.level().isClientSide) this.impactPos = pos;
			} else {
				this.setProjectileMass(Math.max(initialDurabilityMass - durabilityPenalty / 2.0F, 0.0F));
			}

			Vec3 spallDirection = curVel.lengthSqr() > 1.0E-12D
					? curVel.normalize()
					: normal.reverse();
			Vec3 spallLoc = hitLoc.add(spallDirection.scale(2.0D));
			if (!this.level().isClientSide) {
				ImpactExplosion explosion = new ImpactExplosion(
						this.level(),
						this,
						this.indirectArtilleryFire(false),
						spallLoc.x,
						spallLoc.y,
						spallLoc.z,
						2.0F,
						2.0F,
						Explosion.BlockInteraction.KEEP
				);
				CreateBigCannons.handleCustomExplosion(this.level(), explosion);
			}
			SoundType sound = state.getSoundType(this.level(), pos, null);
			if (!this.level().isClientSide) {
				this.level().playSound(null, spallLoc.x, spallLoc.y, spallLoc.z, sound.getBreakSound(), SoundSource.BLOCKS, sound.getVolume(), sound.getPitch());
			}
		}

		this.massLost = ballistics.durabilityMass() < 1.0E-4D
				? 0.0F
				: ((initialDurabilityMass - this.getProjectileMass()) / ballistics.durabilityMass());

		shatter |= this.onImpact(blockHitResult, new ImpactResult(outcome, shatter), projectileContext);
		cir.setReturnValue(new ImpactResult(outcome, shatter));
	}
}