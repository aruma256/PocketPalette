package com.github.aruma256.pocketpalette.client.keybinding;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

public class KeyBase extends KeyMapping {

	protected int pressTime = 0;
	protected int doubleTapTick = 0;
	private static final int DOUBLE_TAP_MAX = 5;

	public KeyBase(String description, int keyCode, String category) {
		super(description, keyCode, category);
	}

	/** Called once per client tick, after the tick has run. */
	public void onEndTick(final Minecraft client) {
		if (this.isDown()) {
			this.pressTime = Math.min(12345, this.pressTime + 1);
			if (this.pressTime == 1) {
				this.onKeyPressStart();
				this.doubleTapTick = DOUBLE_TAP_MAX;
			}
			whilePressed();
		} else {
			if (this.pressTime > 0) {
				this.onKeyReleased();
				this.pressTime = 0;
			}
			if (this.doubleTapTick > 0) {
				this.doubleTapTick--;
			}
		}
	}

	protected void onKeyPressStart() {
	}

	protected void whilePressed() {
	}

	protected void onKeyReleased() {
	}
}
