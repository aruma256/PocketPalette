package com.github.aruma256.pocketpalette.client.gui;

import com.github.aruma256.pocketpalette.client.PocketPaletteClient;
import com.github.aruma256.pocketpalette.client.mixin.AbstractContainerScreenAccessor;
import com.github.aruma256.pocketpalette.client.palette.ItemValidation;
import com.github.aruma256.pocketpalette.client.palette.PaletteConfigManager;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Opens the palette editor when the palette key is pressed while hovering an
 * item in a container screen (JEI/REI-style hover + key interaction).
 */
@Environment(EnvType.CLIENT)
public final class PaletteEditorOpener {

	private PaletteEditorOpener() {}

	public static void register(KeyMapping editorKey) {
		ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
			if (!(screen instanceof AbstractContainerScreen<?>)) {
				return;
			}
			// Consume the editor input before the container can move or drop the hovered item.
			ScreenKeyboardEvents.allowKeyPress(screen).register(
					(s, key, scancode, modifiers) -> !editorKey.matches(key, scancode) || !tryOpen(client, s));
			ScreenMouseEvents.allowMouseClick(screen).register(
					(s, mouseX, mouseY, button) -> !editorKey.matchesMouse(button) || !tryOpen(client, s));
		});
	}

	/** @return whether the editor handled the input, so the container must not process it */
	private static boolean tryOpen(Minecraft client, Screen screen) {
		if (client.player == null || client.player.isSpectator()) {
			return false;
		}
		// Leave input to focused text fields (creative search, anvil names, etc.).
		if (isTextInputFocused(screen)) {
			return false;
		}
		Slot slot = ((AbstractContainerScreenAccessor) screen).pocketpalette$getHoveredSlot();
		if (slot == null || !slot.hasItem()) {
			return false;
		}
		ItemStack stack = slot.getItem();
		if (!ItemValidation.isValidStack(stack)) {
			return false;
		}
		if (!PaletteConfigManager.isEditingAllowed()) {
			PocketPaletteClient.showConfigError("pocketpalette.config.editing_disabled");
			return true;
		}
		// Close the container properly (sends the close packet) before
		// replacing the screen, so the server-side container state stays sane
		client.player.closeContainer();
		client.setScreen(new PaletteEditScreen(stack));
		return true;
	}

	private static boolean isTextInputFocused(GuiEventListener listener) {
		while (listener != null) {
			if (listener instanceof EditBox) {
				return true;
			}
			if (listener instanceof ContainerEventHandler container) {
				listener = container.getFocused();
			} else {
				return false;
			}
		}
		return false;
	}
}
