package org.hectoris919.CBCGoingBallistic.mixin;

import org.hectoris919.CBCGoingBallistic.api.GoingBallisticShotData;
import org.hectoris919.CBCGoingBallistic.api.GoingBallisticShotDataHolder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import rbasamoyai.createbigcannons.munitions.AbstractCannonProjectile;

@Mixin(AbstractCannonProjectile.class)
public abstract class AbstractCannonProjectileShotDataMixin implements GoingBallisticShotDataHolder {
	@Unique
	private GoingBallisticShotData goingballistic$shotData;

	@Override
	public GoingBallisticShotData goingballistic$getShotData() {
		return this.goingballistic$shotData;
	}

	@Override
	public void goingballistic$setShotData(GoingBallisticShotData data) {
		this.goingballistic$shotData = data;
	}
}
