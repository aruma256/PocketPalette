package com.github.aruma256.pocketpalette;

import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;

public class MinecraftBootstrapExtension implements BeforeAllCallback {

    private static boolean initialized = false;

    @Override
    public void beforeAll(ExtensionContext context) {
        if (!initialized) {
            SharedConstants.tryDetectVersion();
            Bootstrap.bootStrap();
            initialized = true;
        }
    }
}
