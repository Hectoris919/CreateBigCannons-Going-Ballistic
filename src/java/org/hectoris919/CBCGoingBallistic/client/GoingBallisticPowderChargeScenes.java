package org.hectoris919.CBCGoingBallistic.client;

import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

public final class GoingBallisticPowderChargeScenes {
	private GoingBallisticPowderChargeScenes() { }

	public static void powderChargeBallistics(SceneBuilder scene, SceneBuildingUtil util) {
		scene.title("powder_charge/going_ballistic", "Powder Charge Ballistics");
		scene.configureBasePlate(0, 0, 5);
		scene.showBasePlate();
		scene.scaleSceneView(0.8f);
		scene.world().showSection(util.select().everywhere(), Direction.UP);
		scene.idle(10);

		BlockPos barrelStart = util.grid().at(1, 1, 2);
		BlockPos barrelEnd = util.grid().at(4, 1, 2);
		BlockPos powderCharge = util.grid().at(2, 1, 2);
		BlockPos projectile = util.grid().at(3, 1, 2);

		scene.world().showSection(util.select().fromTo(barrelStart, barrelEnd), Direction.DOWN);
		scene.idle(15);

		scene.overlay().showText(80)
				.text("Going Ballistic treats Powder Charges as propellant with mass and chemical energy.")
				.colored(PonderPalette.BLUE)
				.pointAt(util.vector().topOf(powderCharge))
				.placeNearTarget()
				.attachKeyFrame();
		scene.idle(90);

		scene.overlay().showText(90)
				.text("Projectile speed is calculated from powder mass, projectile mass, charge length, and barrel length.")
				.colored(PonderPalette.WHITE)
				.pointAt(util.vector().topOf(projectile))
				.placeNearTarget();
		scene.idle(100);

		scene.overlay().showText(90)
				.text("Longer barrels give expanding gas more distance to accelerate the projectile.")
				.colored(PonderPalette.GREEN)
				.pointAt(util.vector().centerOf(4, 1, 2))
				.placeNearTarget();
		scene.idle(100);

		scene.rotateCameraY(35);
		scene.idle(20);

		scene.overlay().showText(90)
				.text("Tooltips now show chemical energy and projectile mass instead of CBC's old flat velocity bonus.")
				.colored(PonderPalette.OUTPUT)
				.pointAt(util.vector().topOf(powderCharge))
				.placeNearTarget()
				.attachKeyFrame();
		scene.idle(100);
		scene.markAsFinished();
	}
}
