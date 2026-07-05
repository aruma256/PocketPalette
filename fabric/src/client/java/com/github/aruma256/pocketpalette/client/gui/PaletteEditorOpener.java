package com.github.aruma256.pocketpalette.client.gui;

import com.github.aruma256.pocketpalette.client.mixin.AbstractContainerScreenAccessor;
import com.github.aruma256.pocketpalette.client.palette.ItemValidation;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
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
			ScreenKeyboardEvents.afterKeyPress(screen).register(
					(s, key, scancode, modifiers) -> tryOpen(client, s, editorKey, key, scancode));
		});
	}

	private static void tryOpen(Minecraft client, Screen screen, KeyMapping editorKey, int key, int scancode) {
		if (!editorKey.matches(key, scancode)) {
			return;
		}
		if (client.player == null || client.player.isSpectator()) {
			return;
		}
		// afterKeyPress fires even when the screen consumed the key, so ignore
		// key presses that were typed into a focused text field
		if (isTextInputFocused(screen)) {
			return;
		}
		Slot slot = ((AbstractContainerScreenAccessor) screen).pocketpalette$getHoveredSlot();
		if (slot == null || !slot.hasItem()) {
			return;
		}
		ItemStack stack = slot.getItem();
		if (!ItemValidation.isValidStack(stack)) {
			return;
		}
		// Close the container properly (sends the close packet) before
		// replacing the screen, so the server-side container state stays sane
		client.player.closeContainer();
		client.setScreen(new PaletteEditScreen(stack));
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
