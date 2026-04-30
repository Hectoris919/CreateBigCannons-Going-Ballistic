package org.hectoris919.CBCGoingBallistic.client;

import net.createmod.ponder.foundation.PonderIndex;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import org.hectoris919.CBCGoingBallistic.GoingBallistic;

@EventBusSubscriber(modid = GoingBallistic.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class GoingBallisticPonderClient {
	private GoingBallisticPonderClient() { }

	@SubscribeEvent
	public static void onClientSetup(FMLClientSetupEvent event) { PonderIndex.addPlugin(new GoingBallisticPonderPlugin()); }
}
