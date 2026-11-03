package com.github.aruma256.pocketpalette.client.compat;

import net.minecraft.client.KeyMapping;

/** Leaves a shared palette binding to LotTweaks in Creative mode. */
public final class LotTweaksCompat {

	private LotTweaksCompat() {}

	public static boolean shouldDisablePaletteKey(boolean creative, KeyMapping paletteKey, KeyMapping[] registeredKeys) {
		if (!creative || paletteKey.isUnbound()) {
			return false;
		}
		// Read the current binding each time so changes in Controls take effect immediately.
		// The mapping only exists while LotTweaks is loaded, so this needs no loader API.
		for (KeyMapping key : registeredKeys) {
			if ("key.lottweaks.palette".equals(key.getName()) && paletteKey.same(key)) {
				return true;
			}
		}
		return false;
	}
}
