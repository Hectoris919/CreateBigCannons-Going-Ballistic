package org.hectoris919.CBCGoingBallistic;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.hectoris919.CBCGoingBallistic.data.BallisticsParameterReloadListener;
import org.hectoris919.CBCGoingBallistic.data.CannonComponentReloadListener;
import org.hectoris919.CBCGoingBallistic.data.ItemProjectileReloadListener;
import org.hectoris919.CBCGoingBallistic.data.ProjectileMassReloadListener;
import org.hectoris919.CBCGoingBallistic.data.PropellantReloadListener;
import org.slf4j.Logger;

@Mod(GoingBallistic.MODID)
public class GoingBallistic {
	public static final String MODID = "cbc_going_ballistic";
	public static final Logger LOGGER = LogUtils.getLogger();

	public GoingBallistic(IEventBus modEventBus, ModContainer modContainer) {
		modEventBus.addListener(this::commonSetup);
		NeoForge.EVENT_BUS.addListener(ProjectileMassReloadListener::onAddReloadListeners);
		NeoForge.EVENT_BUS.addListener(BallisticsParameterReloadListener::onAddReloadListeners);
		NeoForge.EVENT_BUS.addListener(ItemProjectileReloadListener::onAddReloadListeners);
		NeoForge.EVENT_BUS.addListener(PropellantReloadListener::onAddReloadListeners);
		NeoForge.EVENT_BUS.addListener(CannonComponentReloadListener::onAddReloadListeners);
		modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
	}

	private void commonSetup(final FMLCommonSetupEvent event) {
		LOGGER.info("Create Big Cannons: Going Ballistic loaded");
	}
}
