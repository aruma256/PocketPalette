package com.github.aruma256.pocketpalette.client;

import org.lwjgl.glfw.GLFW;

import com.github.aruma256.pocketpalette.PocketPalette;
import com.github.aruma256.pocketpalette.client.gui.PaletteEditorOpener;
import com.github.aruma256.pocketpalette.client.keybinding.PaletteKey;
import com.github.aruma256.pocketpalette.client.palette.PaletteConfigManager;

import net.minecraft.ChatFormatting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Client entry point. {@code dist = Dist.CLIENT} keeps dedicated servers from
 * loading it, so servers do not need the mod installed.
 */
@Mod(value = PocketPalette.MODID, dist = Dist.CLIENT)
public class PocketPaletteClient {

	public static final String KEY_CATEGORY = "key.category.pocketpalette";

	private static final PaletteKey PALETTE_KEY = new PaletteKey(GLFW.GLFW_KEY_R, KEY_CATEGORY);
	private static final ResourceLocation PALETTE_LAYER = ResourceLocation.fromNamespaceAndPath(PocketPalette.MODID, "palette");

	public PocketPaletteClient(IEventBus modEventBus) {
		PaletteConfigManager.loadAllFromFile();

		modEventBus.addListener((RegisterKeyMappingsEvent event) -> event.register(PALETTE_KEY));
		modEventBus.addListener((RegisterGuiLayersEvent event) ->
				event.registerAbove(VanillaGuiLayers.HOTBAR, PALETTE_LAYER, PocketPaletteClient::renderPaletteLayer));

		IEventBus gameBus = NeoForge.EVENT_BUS;
		gameBus.addListener((ClientTickEvent.Post event) -> PALETTE_KEY.onEndTick(Minecraft.getInstance()));
		gameBus.addListener(PocketPaletteClient::onMouseScroll);
		gameBus.addListener(PocketPaletteClient::onLoggingIn);
		gameBus.addListener((RegisterClientCommandsEvent event) -> PocketPaletteCommand.register(event.getDispatcher()));
		PaletteEditorOpener.register(PALETTE_KEY);
	}

	private static void onMouseScroll(InputEvent.MouseScrollingEvent event) {
		// Fired only while no screen is open, before the hotbar selection changes.
		// Spectators scroll the spectator menu instead, so leave that alone.
		LocalPlayer player = Minecraft.getInstance().player;
		if (player == null || player.isSpectator()) {
			return;
		}
		// Like vanilla, fall back to inverted horizontal scroll when there is no vertical scroll.
		double wheel = event.getScrollDeltaY() != 0 ? event.getScrollDeltaY() : -event.getScrollDeltaX();
		if (PALETTE_KEY.onScroll(Math.signum(wheel))) {
			event.setCanceled(true);
		}
	}

	private static void renderPaletteLayer(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
		// Modded layers are not hidden along with the vanilla HUD, so follow F1 like the hotbar does.
		if (Minecraft.getInstance().options.hideGui) {
			return;
		}
		PALETTE_KEY.onRenderHotbar(guiGraphics, deltaTracker.getGameTimeDeltaPartialTick(true));
	}

	private static void onLoggingIn(ClientPlayerNetworkEvent.LoggingIn event) {
		// Reload config now that dynamic registries (enchantments, etc.) are available
		PaletteConfigManager.loadAllFromFile();
		showConfigLoadStatus();
	}

	public static void showConfigLoadStatus() {
		if (!PaletteConfigManager.isEditingAllowed()) {
			showConfigError("pocketpalette.config.editing_disabled");
		}
	}

	public static void showConfigError(String translationKey) {
		Minecraft.getInstance().getChatListener().handleSystemMessage(
				Component.translatable(translationKey).withStyle(ChatFormatting.RED), false);
	}

}
