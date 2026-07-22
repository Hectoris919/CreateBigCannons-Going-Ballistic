package org.hectoris919.CBCGoingBallistic.ballistics;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.hectoris919.CBCGoingBallistic.Config;
import org.hectoris919.CBCGoingBallistic.GoingBallistic;
import org.hectoris919.CBCGoingBallistic.api.GoingBallisticSableImpulseHolder;
import org.hectoris919.CBCGoingBallistic.data.BallisticsParameterRegistry;

public final class SableImpulseMath {
	private static final ThreadLocal<RecoilTransaction> ACTIVE_RECOIL_TRANSACTION = new ThreadLocal<>();
	private static final ThreadLocal<RecoilImpulseOverride> PENDING_RECOIL_OVERRIDE = new ThreadLocal<>();

	private SableImpulseMath() { }

	public static void beginBigCannonShot() {
		beginRecoilTransaction(RecoilShotKind.BIG_CANNON);
	}

	public static void beginAutocannonShot() {
		beginRecoilTransaction(RecoilShotKind.AUTOCANNON);
	}

	private static void beginRecoilTransaction(RecoilShotKind kind) {
		PENDING_RECOIL_OVERRIDE.remove();
		ACTIVE_RECOIL_TRANSACTION.set(new RecoilTransaction(kind));
	}

	public static void endShot() {
		ACTIVE_RECOIL_TRANSACTION.remove();
		PENDING_RECOIL_OVERRIDE.remove();
	}

	public static void recordBigCannonPropellant(double powderMassKg, double velocityMultiplier) {
		RecoilTransaction transaction = transaction(RecoilShotKind.BIG_CANNON);
		if (transaction == null || transaction.finalized) return;
		transaction.addPropellant(powderMassKg, velocityMultiplier);
	}

	public static void recordBigCannonProjectile(double projectileMassKg, double effectivePowderMassKg, double muzzleVelocityMps) {
		RecoilTransaction transaction = transaction(RecoilShotKind.BIG_CANNON);
		if (transaction == null || transaction.finalized) return;
		transaction.recordProjectile(projectileMassKg, effectivePowderMassKg, muzzleVelocityMps);
	}

	public static void finalizeBigCannonRecoil() {
		RecoilTransaction transaction = transaction(RecoilShotKind.BIG_CANNON);
		if (transaction == null) {
			setRecoilOverride(0.0D);
			return;
		}
		finalizeRecoilTransaction(transaction);
	}

	public static void recordAndFinalizeAutocannonShot(double projectileMassKg, double powderMassKg, double muzzleVelocityMps) {
		RecoilTransaction transaction = transaction(RecoilShotKind.AUTOCANNON);
		if (transaction == null) {
			transaction = new RecoilTransaction(RecoilShotKind.AUTOCANNON);
			ACTIVE_RECOIL_TRANSACTION.set(transaction);
		}
		if (transaction.finalized) return;
		transaction.addPropellant(powderMassKg, 1.0D);
		transaction.recordProjectile(projectileMassKg, powderMassKg, muzzleVelocityMps);
		finalizeRecoilTransaction(transaction);
	}

	private static void finalizeRecoilTransaction(RecoilTransaction transaction) {
		if (transaction.finalized) return;
		transaction.finalized = true;

		double kilogramsPerSableMassUnit = Math.max(BallisticsParameterRegistry.kilogramsPerSableMassUnit(), 1.0E-9D);
		double impulseScale = Math.max(BallisticsParameterRegistry.sableRecoilImpulseScale(), 0.0D);

		double projectileImpulseKgMps = 0.0D;
		double gasImpulseKgMps;
		String mode;

		if (transaction.projectileRecorded) {
			mode = "loaded";
			double velocity = positiveFinite(transaction.muzzleVelocityMps);
			double projectileMass = positiveFinite(transaction.projectileMassKg);
			double powderMass = positiveFinite(transaction.effectivePowderMassKg);
			if (powderMass <= 0.0D) powderMass = positiveFinite(transaction.powderMassKg);
			double gasVelocityMultiplier = Math.max(BallisticsParameterRegistry.sableRecoilGasVelocityMultiplier(), 0.0D);

			projectileImpulseKgMps = projectileMass * velocity;
			gasImpulseKgMps = powderMass * gasVelocityMultiplier * velocity;
		} else {
			mode = "blank";
			double powderMass = positiveFinite(transaction.powderMassKg);
			double blankGasVelocity = Math.max(BallisticsParameterRegistry.sableBlankGasVelocityMps(), 0.0D)
					* positiveMultiplier(transaction.propellantVelocityMultiplier);
			double blankEfficiency = clamp(BallisticsParameterRegistry.sableBlankGasImpulseEfficiency());
			gasImpulseKgMps = powderMass * blankGasVelocity * blankEfficiency;
		}

		double impulseSableUnits = sanitizeImpulse(
				((projectileImpulseKgMps + gasImpulseKgMps) / kilogramsPerSableMassUnit) * impulseScale
		);
		setRecoilOverride(impulseSableUnits);
		if (Config.debugBallistics()) {
			GoingBallistic.LOGGER.info(
					"[Going Ballistic] Sable recoil transaction: kind={}, mode={}, projectileRecorded={}, projectileMass={} kg, powderMass={} kg, effectivePowderMass={} kg, muzzleVelocity={} m/s, propellantVelocityMultiplier={}, projectileImpulse={} kg*m/s, gasImpulse={} kg*m/s, kgPerSableMassUnit={}, impulseScale={}, impulse={} kpg*m/s",
					transaction.kind,
					mode,
					transaction.projectileRecorded,
					transaction.projectileMassKg,
					transaction.powderMassKg,
					transaction.effectivePowderMassKg,
					transaction.muzzleVelocityMps,
					transaction.propellantVelocityMultiplier,
					projectileImpulseKgMps,
					gasImpulseKgMps,
					kilogramsPerSableMassUnit,
					impulseScale,
					impulseSableUnits
			);
		}
	}

	private static RecoilTransaction transaction(RecoilShotKind kind) {
		RecoilTransaction transaction = ACTIVE_RECOIL_TRANSACTION.get();
		return transaction != null && transaction.kind == kind ? transaction : null;
	}

	private static void setRecoilOverride(double impulseMagnitude) {
		PENDING_RECOIL_OVERRIDE.set(new RecoilImpulseOverride(true, sanitizeImpulse(impulseMagnitude)));
	}

	public static RecoilImpulseOverride consumeRecoilOverride() {
		RecoilImpulseOverride override = PENDING_RECOIL_OVERRIDE.get();
		PENDING_RECOIL_OVERRIDE.remove();
		return override == null ? RecoilImpulseOverride.NOT_CALCULATED : override;
	}

	public static void captureImpactImpulse(Entity projectile, double projectileMassKg, Vec3 velocityBeforeBlocksPerTick, Vec3 velocityAfterBlocksPerTick) {
		if (!(projectile instanceof GoingBallisticSableImpulseHolder holder)) return;

		double massKg = positiveFinite(projectileMassKg);
		if (massKg <= 0.0D || velocityBeforeBlocksPerTick == null || velocityAfterBlocksPerTick == null) {
			holder.goingballistic$setPendingSableImpactImpulse(0.0D);
			return;
		}

		Vec3 deltaVelocityMps = velocityBeforeBlocksPerTick
				.subtract(velocityAfterBlocksPerTick)
				.scale(BallisticsMath.TICKS_PER_SECOND);
		double deltaSpeedMps = deltaVelocityMps.length();
		double kilogramsPerSableMassUnit = Math.max(BallisticsParameterRegistry.kilogramsPerSableMassUnit(), 1.0E-9D);
		double impulseScale = Math.max(BallisticsParameterRegistry.sableImpactImpulseScale(), 0.0D);
		double impulseSableUnits = (massKg / kilogramsPerSableMassUnit) * deltaSpeedMps * impulseScale;

		if (!Double.isFinite(impulseSableUnits) || impulseSableUnits <= 0.0D) impulseSableUnits = 0.0D;
		holder.goingballistic$setPendingSableImpactImpulse(impulseSableUnits);

		if (Config.debugBallistics()) {
			GoingBallistic.LOGGER.info(
					"[Going Ballistic] Sable impact impulse: projectile={}, mass={} kg, velocityBefore={} blocks/tick, velocityAfter={} blocks/tick, deltaVelocity={} m/s, kgPerSableMassUnit={}, impulseScale={}, impulse={} kpg*m/s",
					BallisticProjectileHelper.getProjectileId(projectile), massKg, velocityBeforeBlocksPerTick, velocityAfterBlocksPerTick, deltaSpeedMps, kilogramsPerSableMassUnit, impulseScale, impulseSableUnits
			);
		}
	}

	public static double consumeImpactImpulse(Entity projectile) {
		if (!(projectile instanceof GoingBallisticSableImpulseHolder holder)) return 0.0D;
		double impulse = holder.goingballistic$consumePendingSableImpactImpulse();
		return !Double.isFinite(impulse) || impulse <= 0.0D ? 0.0D : impulse;
	}

	public static Vec3 bouncedVelocity(Vec3 incomingVelocity, Vec3 surfaceNormal) {
		if (incomingVelocity == null || surfaceNormal == null || surfaceNormal.lengthSqr() <= 1.0E-12D) return Vec3.ZERO;
		Vec3 normal = surfaceNormal.normalize();
		return incomingVelocity.subtract(normal.scale(normal.dot(incomingVelocity) * 1.7D));
	}

	private static double sanitizeImpulse(double value) {
		return Double.isFinite(value) && value > 0.0D ? value : 0.0D;
	}

	private static double positiveFinite(double value) {
		return Double.isFinite(value) && value > 0.0D ? value : 0.0D;
	}

	private static double positiveMultiplier(double value) {
		return Double.isFinite(value) && value > 0.0D ? value : 1.0D;
	}

	private static double clamp(double value) {
		if (!Double.isFinite(value)) return 0.0;
		return Math.max(0.0, Math.min(1.0, value));
	}

	private enum RecoilShotKind {
		BIG_CANNON,
		AUTOCANNON
	}

	private static final class RecoilTransaction {
		private final RecoilShotKind kind;
		private double powderMassKg;
		private double propellantVelocityMultiplier = 1.0D;
		private boolean projectileRecorded;
		private double projectileMassKg;
		private double effectivePowderMassKg;
		private double muzzleVelocityMps;
		private boolean finalized;

		private RecoilTransaction(RecoilShotKind kind) {
			this.kind = kind;
		}

		private void addPropellant(double powderMassKg, double velocityMultiplier) {
			this.powderMassKg += positiveFinite(powderMassKg);
			this.propellantVelocityMultiplier *= positiveMultiplier(velocityMultiplier);
		}

		private void recordProjectile(double projectileMassKg, double effectivePowderMassKg, double muzzleVelocityMps) {
			this.projectileRecorded = true;
			this.projectileMassKg = positiveFinite(projectileMassKg);
			this.effectivePowderMassKg = positiveFinite(effectivePowderMassKg);
			this.muzzleVelocityMps = positiveFinite(Math.abs(muzzleVelocityMps));
		}
	}

	public record RecoilImpulseOverride(boolean calculated, double impulseMagnitude) {
		private static final RecoilImpulseOverride NOT_CALCULATED = new RecoilImpulseOverride(false, 0.0D);

		public RecoilImpulseOverride {
			if (!Double.isFinite(impulseMagnitude) || impulseMagnitude < 0.0D) impulseMagnitude = 0.0D;
		}
	}
}