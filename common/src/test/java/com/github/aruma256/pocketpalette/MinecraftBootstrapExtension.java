package com.github.aruma256.pocketpalette;

import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import net.minecraft.SharedConstants;
import net.minecraft.core.component.DataComponentInitializers;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.registries.VanillaRegistries;
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
                    // Bind vanilla item defaults after loading the registries they depend on (26.1+).
                    // NeoForge's dev-only component validation rejects this lookup's placeholder tags.
                    boolean runningInIde = SharedConstants.IS_RUNNING_IN_IDE;
                    SharedConstants.IS_RUNNING_IN_IDE = false;
                    try {
                        BuiltInRegistries.DATA_COMPONENT_INITIALIZERS.build(VanillaRegistries.createWorldLookup())
                                .forEach(DataComponentInitializers.PendingComponents::apply);
                    } finally {
                        SharedConstants.IS_RUNNING_IN_IDE = runningInIde;
                    }
                    initialized = true;
                }
            }
        }
    }
}
