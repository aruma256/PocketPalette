package com.github.aruma256.pocketpalette.client;

import org.lwjgl.glfw.GLFW;

import com.github.aruma256.pocketpalette.client.gui.PaletteEditorOpener;
import com.github.aruma256.pocketpalette.client.keybinding.PaletteKey;
import com.github.aruma256.pocketpalette.client.palette.PaletteConfigManager;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.chat.Component;

@Environment(EnvType.CLIENT)
public class PocketPaletteClient implements ClientModInitializer, ClientPlayConnectionEvents.Join {

	public static final String KEY_CATEGORY = "key.category.pocketpalette";

	private static final PaletteKey PALETTE_KEY = new PaletteKey(GLFW.GLFW_KEY_R, KEY_CATEGORY);

	@Override
	public void onInitializeClient() {
		PaletteConfigManager.loadAllFromFile();

		KeyBindingHelper.registerKeyBinding(PALETTE_KEY);
		ClientTickEvents.END_CLIENT_TICK.register(PALETTE_KEY);
		PaletteEditorOpener.register(PALETTE_KEY);

		ClientPlayConnectionEvents.JOIN.register(this);
		ClientCommandRegistrationCallback.EVENT.register(new PocketPaletteCommand());
	}

	public static boolean onScroll(double scrollAmount) {
		return PALETTE_KEY.onScroll(scrollAmount);
	}

	public static void onRenderHotbar(GuiGraphics guiGraphics, float partialTicks) {
		PALETTE_KEY.onRenderHotbar(guiGraphics, partialTicks);
	}

	public static void showConfigLoadStatus() {
		if (!PaletteConfigManager.isEditingAllowed()) {
			showConfigError("pocketpalette.config.editing_disabled");
		}
	}

	@Override
	public void onPlayReady(ClientPacketListener handler, PacketSender sender, Minecraft client) {
		// Reload config now that dynamic registries (enchantments, etc.) are available
		PaletteConfigManager.loadAllFromFile();
		showConfigLoadStatus();
	}

	public static void showConfigError(String translationKey) {
		Minecraft.getInstance().getChatListener().handleSystemMessage(
				Component.translatable(translationKey).withStyle(ChatFormatting.RED), false);
	}

}
