package com.github.aruma256.pocketpalette.client.gui;

import com.github.aruma256.pocketpalette.client.PocketPaletteClient;
import com.github.aruma256.pocketpalette.client.palette.ItemValidation;
import com.github.aruma256.pocketpalette.client.palette.PaletteConfigManager;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Opens the palette editor when the palette key is pressed while hovering an
 * item in a container screen (JEI/REI-style hover + key interaction).
 */
public final class PaletteEditorOpener {

	private PaletteEditorOpener() {}

	public static void register(KeyMapping editorKey) {
		// Consume the editor input before the container can move or drop the hovered item.
		NeoForge.EVENT_BUS.addListener((ScreenEvent.KeyPressed.Pre event) -> {
			if (editorKey.matches(event.getKeyCode(), event.getScanCode()) && tryOpen(event.getScreen())) {
				event.setCanceled(true);
			}
		});
		NeoForge.EVENT_BUS.addListener((ScreenEvent.MouseButtonPressed.Pre event) -> {
			if (editorKey.matchesMouse(event.getButton()) && tryOpen(event.getScreen())) {
				event.setCanceled(true);
			}
		});
	}

	/** @return whether the editor handled the input, so the container must not process it */
	private static boolean tryOpen(Screen screen) {
		if (!(screen instanceof AbstractContainerScreen<?> containerScreen)) {
			return false;
		}
		Minecraft client = Minecraft.getInstance();
		if (client.player == null || client.player.isSpectator()) {
			return false;
		}
		// Leave input to focused text fields (creative search, anvil names, etc.).
		if (isTextInputFocused(screen)) {
			return false;
		}
		Slot slot = containerScreen.getSlotUnderMouse();
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

	private static boolean isTextInputFocused(Screen screen) {
		// The creative search box takes focus via EditBox#setFocused only, outside the focus chain.
		for (GuiEventListener child : screen.children()) {
			if (child instanceof EditBox editBox && editBox.canConsumeInput()) {
				return true;
			}
		}
		GuiEventListener listener = screen;
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
