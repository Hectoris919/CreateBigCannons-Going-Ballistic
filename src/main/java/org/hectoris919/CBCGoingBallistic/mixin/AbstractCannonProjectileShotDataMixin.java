package org.hectoris919.CBCGoingBallistic.mixin;

import org.hectoris919.CBCGoingBallistic.api.GoingBallisticShotData;
import org.hectoris919.CBCGoingBallistic.api.GoingBallisticShotDataHolder;
import org.hectoris919.CBCGoingBallistic.api.GoingBallisticSableImpulseHolder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import rbasamoyai.createbigcannons.munitions.AbstractCannonProjectile;

@Mixin(AbstractCannonProjectile.class)
public abstract class AbstractCannonProjectileShotDataMixin implements GoingBallisticShotDataHolder, GoingBallisticSableImpulseHolder {
	@Unique
	private GoingBallisticShotData goingballistic$shotData;

	@Unique
	private double goingballistic$pendingSableImpactImpulse;

	@Override
	public GoingBallisticShotData goingballistic$getShotData() {
		return this.goingballistic$shotData;
	}

	@Override
	public void goingballistic$setShotData(GoingBallisticShotData data) {
		this.goingballistic$shotData = data;
	}

	@Override
	public void goingballistic$setPendingSableImpactImpulse(double impulseMagnitude) {
		this.goingballistic$pendingSableImpactImpulse = Double.isFinite(impulseMagnitude) && impulseMagnitude > 0.0D
				? impulseMagnitude
				: 0.0D;
	}

	@Override
	public double goingballistic$consumePendingSableImpactImpulse() {
		double impulseMagnitude = this.goingballistic$pendingSableImpactImpulse;
		this.goingballistic$pendingSableImpactImpulse = 0.0D;
		return impulseMagnitude;
	}
}