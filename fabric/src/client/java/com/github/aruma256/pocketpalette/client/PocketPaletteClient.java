package com.github.aruma256.pocketpalette.client;

import org.lwjgl.glfw.GLFW;

import com.github.aruma256.pocketpalette.PocketPalette;
import com.github.aruma256.pocketpalette.client.gui.PaletteEditorOpener;
import com.github.aruma256.pocketpalette.client.keybinding.PaletteKey;
import com.github.aruma256.pocketpalette.client.palette.PaletteConfigManager;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

@Environment(EnvType.CLIENT)
public class PocketPaletteClient implements ClientModInitializer, ClientPlayConnectionEvents.Join {

	public static final KeyMapping.Category KEY_CATEGORY = KeyMapping.Category.register(
			Identifier.fromNamespaceAndPath(PocketPalette.MODID, "main"));

	private static final PaletteKey PALETTE_KEY = new PaletteKey(GLFW.GLFW_KEY_R, KEY_CATEGORY);

	@Override
	public void onInitializeClient() {
		KeyMappingHelper.registerKeyMapping(PALETTE_KEY);
		ClientTickEvents.END_CLIENT_TICK.register(PALETTE_KEY);
		PaletteEditorOpener.register(PALETTE_KEY);

		ClientPlayConnectionEvents.JOIN.register(this);
		ClientCommandRegistrationCallback.EVENT.register(new PocketPaletteCommand());
	}

	public static boolean onScroll(double scrollAmount) {
		return PALETTE_KEY.onScroll(scrollAmount);
	}

	public static void onRenderHotbar(GuiGraphicsExtractor guiGraphics, float partialTicks) {
		PALETTE_KEY.onRenderHotbar(guiGraphics, partialTicks);
	}

	public static void showConfigLoadStatus() {
		if (!PaletteConfigManager.isEditingAllowed()) {
			showConfigError("pocketpalette.config.editing_disabled");
		}
	}

	@Override
	public void onPlayReady(ClientPacketListener handler, PacketSender sender, Minecraft client) {
		// ItemStack defaults and dynamic registries are only available after joining (26.1+).
		PaletteConfigManager.loadAllFromFile();
		showConfigLoadStatus();
	}

	public static void showConfigError(String translationKey) {
		Minecraft.getInstance().getChatListener().handleSystemMessage(
				Component.translatable(translationKey).withStyle(ChatFormatting.RED), false);
	}

}
