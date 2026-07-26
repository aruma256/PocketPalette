package com.github.aruma256.pocketpalette;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Shared constants for PocketPalette.
 * This is a client-side only mod; there is no server entry point.
 */
public class PocketPalette {

	public static final String MODID = "pocketpalette";
	public static final String NAME = "PocketPalette";
	public static final String VERSION = "0.1.0";
	public static final Logger LOGGER = LoggerFactory.getLogger(MODID);

	public static class CONFIG {
		public static boolean DISABLE_ANIMATIONS = false;
		public static boolean SNEAK_TO_SWITCH_GROUP = false;
		public static boolean SHOW_CONFIG_ERROR_LOG_TO_CHAT = true;
	}

}
