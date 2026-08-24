package org.hectoris919.CBCGoingBallistic.mixin;

import org.hectoris919.CBCGoingBallistic.Config;
import org.hectoris919.CBCGoingBallistic.ballistics.BallisticProjectileHelper;
import org.hectoris919.CBCGoingBallistic.ballistics.BlockDamageMath;
import org.hectoris919.CBCGoingBallistic.ballistics.BlockDamageMath.ImpactSolution;
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

	@Inject(method = "calculateBlockPenetration", at = @At("HEAD"), cancellable = true, order = 2000)
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
		double massKg = BallisticProjectileHelper.getProjectileMassKg(this);

		double toughness = blockArmor.toughness(this.level(), state, pos, true);
		double hardness = blockArmor.hardness(this.level(), state, pos, true);
		boolean surfaceImpact = this.canHitSurface();
		boolean canBounce = CBCConfigs.server().munitions.projectilesCanBounce.get();
		double bounceRoll = surfaceImpact && canBounce ? this.level().getRandom().nextDouble() : 1.0D;
		float initialDurabilityMass = this.getProjectileMass();

		ImpactSolution impact = BlockDamageMath.solveImpact(
				curVel,
				normal,
				massKg,
				toughness,
				hardness,
				ballistics.penetration(),
				ballistics.toughness(),
				ballistics.deflection(),
				initialDurabilityMass,
				surfaceImpact,
				canBounce,
				unbreakable,
				!this.level().isClientSide,
				CBCConfigs.server().munitions.baseProjectileBounceChance.getF(),
				bounceRoll
		);

		ImpactResult.KinematicOutcome outcome = impact.outcome();
		boolean blockBroken = impact.blockBroken();
		boolean shatter = impact.shatter();

		SableImpulseMath.captureImpactImpulse(this, massKg, curVel, impact.postImpactVelocity());

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
			if (impact.partialDamagePoints() > 1.0E-6D) {
				CreateBigCannons.BLOCK_DAMAGE.damageBlock(pos.immutable(), (float) impact.partialDamagePoints(), state, this.level());
			}
		}

		BlockDamageMath.logImpact("Big cannon", this, massKg, impact);

		if (blockBroken) {
			this.setDeltaMovement(impact.postImpactVelocity());
			this.setProjectileMass(Math.max(initialDurabilityMass - impact.durabilityPenalty(), 0.0F));
			this.level().setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL_IMMEDIATE);
			if (surfaceImpact) {
				float workRatio = impact.impactEnergyJ() <= 1.0E-4D
						? 1.0F
						: (float) (impact.requiredWorkJ() / impact.impactEnergyJ());
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
				this.setProjectileMass(Math.max(initialDurabilityMass - impact.durabilityPenalty() / 2.0F, 0.0F));
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