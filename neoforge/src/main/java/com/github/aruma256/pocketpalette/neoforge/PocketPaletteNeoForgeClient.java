package com.github.aruma256.pocketpalette.neoforge;

import com.github.aruma256.pocketpalette.PocketPalette;
import com.github.aruma256.pocketpalette.client.PocketPaletteClient;
import com.github.aruma256.pocketpalette.client.PocketPaletteCommand;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.player.LocalPlayer;
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
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.common.NeoForge;

/**
 * NeoForge entry point: connects NeoForge events to {@link PocketPaletteClient}.
 * {@code dist = Dist.CLIENT} keeps dedicated servers from loading it, so servers
 * do not need the mod installed.
 */
@Mod(value = PocketPalette.MODID, dist = Dist.CLIENT)
public class PocketPaletteNeoForgeClient {

	private static final ResourceLocation PALETTE_LAYER = ResourceLocation.fromNamespaceAndPath(PocketPalette.MODID, "palette");

	public PocketPaletteNeoForgeClient(IEventBus modEventBus) {
		PocketPaletteClient.onInitialize();

		modEventBus.addListener((RegisterKeyMappingsEvent event) -> event.register(PocketPaletteClient.getPaletteKey()));
		modEventBus.addListener((RegisterGuiLayersEvent event) ->
				event.registerAbove(VanillaGuiLayers.HOTBAR, PALETTE_LAYER, PocketPaletteNeoForgeClient::renderPaletteLayer));

		IEventBus gameBus = NeoForge.EVENT_BUS;
		gameBus.addListener((ClientTickEvent.Post event) -> PocketPaletteClient.onEndTick(Minecraft.getInstance()));
		gameBus.addListener(PocketPaletteNeoForgeClient::onMouseScroll);
		gameBus.addListener(PocketPaletteNeoForgeClient::onContainerKeyPressed);
		gameBus.addListener(PocketPaletteNeoForgeClient::onContainerMouseClicked);
		gameBus.addListener((ClientPlayerNetworkEvent.LoggingIn event) -> PocketPaletteClient.onJoinWorld());
		gameBus.addListener((RegisterClientCommandsEvent event) -> PocketPaletteCommand.register(event.getDispatcher()));
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
		if (PocketPaletteClient.onScroll(Math.signum(wheel))) {
			event.setCanceled(true);
		}
	}

	private static void renderPaletteLayer(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
		// Modded layers are not hidden along with the vanilla HUD, so follow F1 like the hotbar does.
		if (Minecraft.getInstance().options.hideGui) {
			return;
		}
		PocketPaletteClient.onRenderHotbar(guiGraphics, deltaTracker.getGameTimeDeltaPartialTick(true));
	}

	private static void onContainerKeyPressed(ScreenEvent.KeyPressed.Post event) {
		// Post fires only for keys the screen left unhandled.
		if (event.getScreen() instanceof AbstractContainerScreen<?> screen
				&& PocketPaletteClient.onContainerKeyPressed(screen.getSlotUnderMouse(), event.getKeyCode(), event.getScanCode())) {
			event.setCanceled(true);
		}
	}

	private static void onContainerMouseClicked(ScreenEvent.MouseButtonPressed.Pre event) {
		if (event.getScreen() instanceof AbstractContainerScreen<?> screen
				&& PocketPaletteClient.onContainerMouseClicked(screen.getSlotUnderMouse(), event.getButton())) {
			event.setCanceled(true);
		}
	}

}
