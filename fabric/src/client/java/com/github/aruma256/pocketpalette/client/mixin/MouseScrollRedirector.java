package com.github.aruma256.pocketpalette.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.github.aruma256.pocketpalette.client.PocketPaletteClient;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.client.MouseHandler;

@Mixin(MouseHandler.class)
public abstract class MouseScrollRedirector {

	@WrapOperation(method = "onScroll", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/ScrollWheelHandler;getNextScrollWheelSelection(DII)I"))
	private int pocketpalette_redirectedScrollWheelSelection(double rawScrollAmount, int currentSelection, int maxSelections, Operation<Integer> original) {
		int scrollAmount = (int)Math.signum(rawScrollAmount);
		if (PocketPaletteClient.onScroll(scrollAmount)) {
			return currentSelection; // キャンセルされた場合は現在のスロットをそのまま返す
		}
		return original.call(rawScrollAmount, currentSelection, maxSelections);
	}

}
