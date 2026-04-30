package org.hectoris919.CBCGoingBallistic.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import org.hectoris919.CBCGoingBallistic.GoingBallistic;

import java.util.List;
import java.util.Locale;

@EventBusSubscriber(modid = GoingBallistic.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.GAME)
public final class GoingBallisticClientTooltipEvents {
	private GoingBallisticClientTooltipEvents() { }

	@SubscribeEvent
	public static void onItemTooltip(ItemTooltipEvent event) {
		List<Component> tooltip = event.getToolTip();
		boolean shiftDown = Screen.hasShiftDown();

		appendShiftHintIfNeeded(event, tooltip, shiftDown);

		if (!shiftDown) return;
		removeLegacyVelocitySummaryLines(tooltip);
		BallisticsTooltipApi.appendGoingBallisticTooltip(event.getItemStack(), tooltip);
	}

	private static void appendShiftHintIfNeeded(ItemTooltipEvent event, List<Component> tooltip, boolean shiftDown) {
		if (!BallisticsTooltipApi.hasGoingBallisticTooltip(event.getItemStack())) return;

		String joinedTooltip = tooltip.stream()
				.map(Component::getString)
				.map(s -> s.toLowerCase(Locale.ROOT))
				.reduce("", (a, b) -> a + "\n" + b);

		if (joinedTooltip.contains("hold") && joinedTooltip.contains("shift") && joinedTooltip.contains("summary")) return;

		tooltip.add(buildShiftSummaryHint(shiftDown));
	}

	private static Component buildShiftSummaryHint(boolean shiftDown) {
		return Component.literal("Hold [").withStyle(ChatFormatting.DARK_GRAY)
				.append(Component.literal("Shift").withStyle(shiftDown ? ChatFormatting.WHITE : ChatFormatting.GRAY))
				.append(Component.literal("] for Summary").withStyle(ChatFormatting.DARK_GRAY));
	}

	private static void removeLegacyVelocitySummaryLines(List<Component> tooltip) {
		for (int i = tooltip.size() - 1; i >= 0; --i) {
			String text = tooltip.get(i).getString().toLowerCase(Locale.ROOT);
			if (isLegacyVelocityHeader(text)) {
				tooltip.remove(i);
				if (i < tooltip.size() && looksLikeLegacyVelocityValue(tooltip.get(i).getString())) {
					tooltip.remove(i);
				}
			}
		}
	}

	private static boolean isLegacyVelocityHeader(String lowerText) {
		return lowerText.contains("added muzzle velocity")
				|| lowerText.contains("added velocity")
				|| lowerText.contains("muzzle velocity added")
				|| lowerText.contains("maximum firing speed");
	}

	private static boolean looksLikeLegacyVelocityValue(String text) {
		String lower = text.toLowerCase(Locale.ROOT).trim();
		return lower.startsWith("+")
				|| lower.contains("blocks/tick")
				|| lower.contains("m/s")
				|| lower.contains("velocity")
				|| lower.contains("speed")
				|| lower.matches(".*\\d.*");
	}
}
