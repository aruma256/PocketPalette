package com.github.aruma256.pocketpalette.fabric;

import com.github.aruma256.pocketpalette.client.PocketPaletteClient;
import com.github.aruma256.pocketpalette.client.PocketPaletteCommand;
import com.github.aruma256.pocketpalette.fabric.mixin.AbstractContainerScreenAccessor;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

/** Fabric entry point: connects Fabric API events to {@link PocketPaletteClient}. Mixins cover the rest. */
@Environment(EnvType.CLIENT)
public class PocketPaletteFabricClient implements ClientModInitializer {

	@Override
	public void onInitializeClient() {
		KeyMapping.Category.register(PocketPaletteClient.KEY_CATEGORY.id());
		KeyMappingHelper.registerKeyMapping(PocketPaletteClient.getPaletteKey());
		ClientTickEvents.END_CLIENT_TICK.register(PocketPaletteClient::onEndTick);
		ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
			if (!(screen instanceof AbstractContainerScreen<?>)) {
				return;
			}
			ScreenMouseEvents.allowMouseClick(screen).register((s, event) ->
					!PocketPaletteClient.onContainerMouseClicked(((AbstractContainerScreenAccessor) s).pocketpalette$getHoveredSlot(), event));
		});

		ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> PocketPaletteClient.onJoinWorld());
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> PocketPaletteCommand.register(dispatcher));
	}

}
