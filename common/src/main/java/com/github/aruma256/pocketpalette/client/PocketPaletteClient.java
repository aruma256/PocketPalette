package com.github.aruma256.pocketpalette.client;

import org.lwjgl.glfw.GLFW;

import com.github.aruma256.pocketpalette.PocketPalette;
import com.github.aruma256.pocketpalette.client.gui.PaletteEditorOpener;
import com.github.aruma256.pocketpalette.client.keybinding.PaletteKey;
import com.github.aruma256.pocketpalette.client.palette.PaletteConfigManager;

import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.Slot;

/**
 * Loader-independent client logic. Each loader's entry point turns its own events
 * and hooks into calls on this class; the shared code never calls back into a loader.
 */
public final class PocketPaletteClient {

	/** The key category the loader must register. */
	public static final KeyMapping.Category KEY_CATEGORY = new KeyMapping.Category(
			Identifier.fromNamespaceAndPath(PocketPalette.MODID, "main"));

	private static final PaletteKey PALETTE_KEY = new PaletteKey(GLFW.GLFW_KEY_R, KEY_CATEGORY);

	private PocketPaletteClient() {}

	/** The key mapping the loader must register. */
	public static KeyMapping getPaletteKey() {
		return PALETTE_KEY;
	}

	public static void onEndTick(Minecraft client) {
		PALETTE_KEY.onEndTick(client);
	}

	/** @return whether the palette consumed the scroll, so the hotbar selection must not change */
	public static boolean onScroll(double scrollAmount) {
		return PALETTE_KEY.onScroll(scrollAmount);
	}

	public static void onRenderHotbar(GuiGraphicsExtractor guiGraphics, float partialTicks) {
		PALETTE_KEY.onRenderHotbar(guiGraphics, partialTicks);
	}

	/**
	 * Call only for keys the container screen left unhandled, so text fields take priority.
	 *
	 * @param hoveredSlot the slot under the mouse, or null
	 * @return whether the editor handled the input, so the container must not process it
	 */
	public static boolean onContainerKeyPressed(Slot hoveredSlot, KeyEvent event) {
		return PALETTE_KEY.isEnabled() && PALETTE_KEY.matches(event) && PaletteEditorOpener.tryOpen(hoveredSlot);
	}

	/**
	 * Call before the container processes the click, so it cannot move or drop the hovered item.
	 *
	 * @param hoveredSlot the slot under the mouse, or null
	 * @return whether the editor handled the input, so the container must not process it
	 */
	public static boolean onContainerMouseClicked(Slot hoveredSlot, MouseButtonEvent event) {
		return PALETTE_KEY.isEnabled() && PALETTE_KEY.matchesMouse(event) && PaletteEditorOpener.tryOpen(hoveredSlot);
	}

	public static void onJoinWorld() {
		// ItemStack defaults and dynamic registries are only available after joining (26.1+).
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
