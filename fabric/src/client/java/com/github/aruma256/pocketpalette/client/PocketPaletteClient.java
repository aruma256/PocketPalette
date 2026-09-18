package com.github.aruma256.pocketpalette.client;

import org.lwjgl.glfw.GLFW;

import com.github.aruma256.pocketpalette.client.event.RenderHotbarEvent;
import com.github.aruma256.pocketpalette.client.event.RenderHotbarEvent.RenderHotbarListener;
import com.github.aruma256.pocketpalette.client.event.ScrollEvent;
import com.github.aruma256.pocketpalette.client.event.ScrollEvent.ScrollListener;
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
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.chat.Component;

@Environment(EnvType.CLIENT)
public class PocketPaletteClient implements ClientModInitializer, ClientPlayConnectionEvents.Join {

	public static final String KEY_CATEGORY = "key.category.pocketpalette";

	private static final PaletteKey PALETTE_KEY = new PaletteKey(GLFW.GLFW_KEY_R, KEY_CATEGORY);

	private static final KeyMapping[] KEY_MAPPINGS = {
			PALETTE_KEY
	};

	@Override
	public void onInitializeClient() {
		PaletteConfigManager.loadAllFromFile();
		//
		for (KeyMapping key : KEY_MAPPINGS) {
			registerKey(key);
		}
		//
		PaletteEditorOpener.register(PALETTE_KEY);
		//
		ClientPlayConnectionEvents.JOIN.register(this);
		//
		ClientCommandRegistrationCallback.EVENT.register(new PocketPaletteCommand());
	}

	private static void registerToMyEventBus(Object obj) {
		if (obj instanceof ClientTickEvents.EndTick) {
			ClientTickEvents.END_CLIENT_TICK.register((ClientTickEvents.EndTick)obj);
		}
		if (obj instanceof RenderHotbarListener) {
			RenderHotbarEvent.registerListener((RenderHotbarListener)obj);
		}
		if (obj instanceof ScrollListener) {
			ScrollEvent.registerListener((ScrollListener)obj);
		}
	}

	private static void registerKey(KeyMapping key) {
		registerToMyEventBus(key);
		KeyBindingHelper.registerKeyBinding(key);
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
