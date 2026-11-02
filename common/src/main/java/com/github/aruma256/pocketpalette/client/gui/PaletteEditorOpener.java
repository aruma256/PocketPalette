package com.github.aruma256.pocketpalette.client.gui;

import com.github.aruma256.pocketpalette.client.PocketPaletteClient;
import com.github.aruma256.pocketpalette.client.palette.ItemValidation;
import com.github.aruma256.pocketpalette.client.palette.PaletteConfigManager;

import net.minecraft.client.Minecraft;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Opens the palette editor when the palette key is pressed while hovering an
 * item in a container screen (JEI/REI-style hover + key interaction).
 */
public final class PaletteEditorOpener {

	private PaletteEditorOpener() {}

	public static boolean tryOpen(Slot slot) {
		Minecraft client = Minecraft.getInstance();
		if (client.player == null || client.player.isSpectator()) {
			return false;
		}
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
}
