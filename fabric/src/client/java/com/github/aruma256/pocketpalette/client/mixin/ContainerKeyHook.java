package com.github.aruma256.pocketpalette.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.github.aruma256.pocketpalette.client.PocketPaletteClient;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

@Mixin(AbstractContainerScreen.class)
public abstract class ContainerKeyHook {

	// Screens with text fields consume typed keys before reaching this call, so they take priority.
	@WrapOperation(method = "keyPressed", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;keyPressed(III)Z"))
	private boolean pocketpalette_openEditorOnUnhandledKey(AbstractContainerScreen<?> screen, int key, int scancode, int modifiers, Operation<Boolean> original) {
		return original.call(screen, key, scancode, modifiers) || PocketPaletteClient.onContainerKeyPressed(screen, key, scancode);
	}

}
