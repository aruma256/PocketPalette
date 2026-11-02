package com.github.aruma256.pocketpalette.fabric.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.github.aruma256.pocketpalette.client.PocketPaletteClient;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;

@Mixin(AbstractContainerScreen.class)
public abstract class ContainerKeyHook {

	// Screens with text fields consume typed keys before reaching this call.
	@WrapOperation(method = "keyPressed", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;keyPressed(Lnet/minecraft/client/input/KeyEvent;)Z"))
	private boolean pocketpalette_openEditorOnUnhandledKey(AbstractContainerScreen<?> screen, KeyEvent event, Operation<Boolean> original) {
		return original.call(screen, event)
				|| PocketPaletteClient.onContainerKeyPressed(((AbstractContainerScreenAccessor) screen).pocketpalette$getHoveredSlot(), event);
	}

}
