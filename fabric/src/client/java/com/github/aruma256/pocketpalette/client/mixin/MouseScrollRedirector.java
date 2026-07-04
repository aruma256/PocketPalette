package com.github.aruma256.pocketpalette.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.github.aruma256.pocketpalette.client.event.ScrollEvent;

import net.minecraft.client.MouseHandler;
import net.minecraft.client.ScrollWheelHandler;

@Mixin(MouseHandler.class)
public abstract class MouseScrollRedirector {

	@Redirect(method = "onScroll", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/ScrollWheelHandler;getNextScrollWheelSelection(DII)I"))
	private int pocketpalette_redirectedScrollWheelSelection(double rawScrollAmount, int currentSelection, int maxSelections) {
		int scrollAmount = (int)Math.signum(rawScrollAmount);
		if (ScrollEvent.post(new ScrollEvent(scrollAmount))) {
			return currentSelection; // キャンセルされた場合は現在のスロットをそのまま返す
		}
		return ScrollWheelHandler.getNextScrollWheelSelection(rawScrollAmount, currentSelection, maxSelections);
	}

}
