package org.hectoris919.CBCGoingBallistic.client;

import net.createmod.ponder.api.registration.PonderPlugin;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class GoingBallisticPonderPlugin implements PonderPlugin {
	public static final String PONDER_NAMESPACE = "cbc_going_ballistic";
	private static final ResourceLocation POWDER_CHARGE_ID = ResourceLocation.parse("createbigcannons:powder_charge");

	@Override
	public @NotNull String getModId() { return PONDER_NAMESPACE; }

	@Override
	public void registerScenes(PonderSceneRegistrationHelper helper) {
		helper.forComponents(List.of(POWDER_CHARGE_ID))
				.addStoryBoard(
					"powder_charge/going_ballistic",
					GoingBallisticPowderChargeScenes::powderChargeBallistics
				);
	}
}
