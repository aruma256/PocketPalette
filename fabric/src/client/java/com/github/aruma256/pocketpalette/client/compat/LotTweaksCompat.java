package com.github.aruma256.pocketpalette.client.compat;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;

/** Leaves a shared palette binding to LotTweaks in Creative mode. */
@Environment(EnvType.CLIENT)
public final class LotTweaksCompat {

	private final boolean loaded;

	public LotTweaksCompat() {
		this(FabricLoader.getInstance().isModLoaded("lottweaks"));
	}

	LotTweaksCompat(boolean loaded) {
		this.loaded = loaded;
	}

	public boolean shouldDisablePaletteKey(boolean creative, KeyMapping paletteKey, KeyMapping[] registeredKeys) {
		if (!loaded || !creative || paletteKey.isUnbound()) {
			return false;
		}
		// Read the current binding each time so changes in Controls take effect immediately.
		for (KeyMapping key : registeredKeys) {
			if ("key.lottweaks.palette".equals(key.getName()) && paletteKey.same(key)) {
				return true;
			}
		}
		return false;
	}
}
