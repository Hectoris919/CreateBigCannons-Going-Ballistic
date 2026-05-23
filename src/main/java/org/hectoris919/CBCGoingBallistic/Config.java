package org.hectoris919.CBCGoingBallistic;

import net.neoforged.neoforge.common.ModConfigSpec;

public class Config {
	private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

	private static final ModConfigSpec.BooleanValue DISABLE_REALISTIC_BALLISTICS = BUILDER
			.comment("Disables Going Ballistic's velocity calculations.")
			.define("disableRealisticBallistics", false);

	private static final ModConfigSpec.BooleanValue DISABLE_REALISTIC_BLOCK_DAMAGE = BUILDER
			.comment("Disables Going Ballistic's impact-energy block damage calculation.")
			.define("disableRealisticBlockDamage", false);

	private static final ModConfigSpec.BooleanValue DISABLE_REALISTIC_PROJECTILE_GRAVITY = BUILDER
			.comment("Disables Going Ballistic's Earth-gravity projectile override.")
			.define("projectilePhysics.disableRealisticGravity", false);

	private static final ModConfigSpec.BooleanValue DISABLE_REALISTIC_PROJECTILE_DRAG = BUILDER
			.comment("Disables Going Ballistic's quadratic projectile drag override.")
			.define("projectilePhysics.disableRealisticDrag", false);

	private static final ModConfigSpec.BooleanValue DEBUG_BALLISTICS = BUILDER
			.comment("Logs debug information whenever a shot or impact is calculated.")
			.define("debugBallistics", false);

	public static final ModConfigSpec SPEC = BUILDER.build();

	public static boolean disableRealisticBallistics() { return DISABLE_REALISTIC_BALLISTICS.get(); }
	public static boolean disableRealisticBlockDamage() { return DISABLE_REALISTIC_BLOCK_DAMAGE.get(); }
	public static boolean disableRealisticProjectileGravity() { return DISABLE_REALISTIC_PROJECTILE_GRAVITY.get(); }
	public static boolean disableRealisticProjectileDrag() { return DISABLE_REALISTIC_PROJECTILE_DRAG.get(); }
	public static boolean debugBallistics() { return DEBUG_BALLISTICS.get(); }
}
