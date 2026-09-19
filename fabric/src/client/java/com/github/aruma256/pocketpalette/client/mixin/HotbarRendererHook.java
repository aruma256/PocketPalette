package com.github.aruma256.pocketpalette.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.github.aruma256.pocketpalette.client.PocketPaletteClient;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;

@Mixin(Gui.class)
public abstract class HotbarRendererHook {

	@Inject(at = @At("TAIL"), method = "renderItemHotbar(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/DeltaTracker;)V")
	private void pocketpalette_renderHotbar(GuiGraphics guiGraphics, DeltaTracker deltaTracker, CallbackInfo info) {
		PocketPaletteClient.onRenderHotbar(guiGraphics, deltaTracker.getGameTimeDeltaPartialTick(true));
	}

}
