package com.github.aruma256.pocketpalette;

import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;

/**
 * Initializes the Minecraft game registries (items, components, ...) once
 * per JVM so that unit tests can construct ItemStacks and use codecs.
 */
public class MinecraftBootstrapExtension implements BeforeAllCallback {

    private static volatile boolean initialized = false;

    @Override
    public void beforeAll(ExtensionContext context) {
        if (!initialized) {
            synchronized (MinecraftBootstrapExtension.class) {
                if (!initialized) {
                    SharedConstants.tryDetectVersion();
                    Bootstrap.bootStrap();
                    initialized = true;
                }
            }
        }
    }
}
