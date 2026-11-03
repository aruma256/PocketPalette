package com.github.aruma256.pocketpalette.neoforge.test;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import com.github.aruma256.pocketpalette.PocketPalette;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;

/** Runs the client game test once the title screen is up, then ends the JVM with its result. */
@Mod(value = "pocketpalette_test", dist = Dist.CLIENT)
public final class ClientGameTestMod {
    private static final long WATCHDOG_MILLIS = TimeUnit.MINUTES.toMillis(10);

    public ClientGameTestMod() {
        startDaemon("watchdog", ClientGameTestMod::watchdog);
        ClientGameTestContext context = new ClientGameTestContext();
        AtomicBoolean started = new AtomicBoolean();
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post event) -> {
            context.onTick();
            if (Minecraft.getInstance().gui.screen() instanceof TitleScreen && started.compareAndSet(false, true)) {
                startDaemon("runner", () -> run(context));
            }
        });
    }

    private static void run(ClientGameTestContext context) {
        try {
            context.blockRealInput();
            context.createWorld();
            PaletteClientGameTest.run(context);
        } catch (Throwable t) {
            PocketPalette.LOGGER.error("Client game test failed", t);
            // A normal shutdown exits with 0. Set the code only after the window is gone,
            // because halting under a live window crashes the JVM on Windows.
            Runtime.getRuntime().addShutdownHook(new Thread(() -> Runtime.getRuntime().halt(1)));
        }
        Minecraft client = Minecraft.getInstance();
        client.execute(client::stop);
    }

    private static void watchdog() {
        try {
            Thread.sleep(WATCHDOG_MILLIS);
        } catch (InterruptedException e) {
            return;
        }
        PocketPalette.LOGGER.error("Client game test timed out");
        Runtime.getRuntime().halt(2);
    }

    private static void startDaemon(String name, Runnable task) {
        Thread thread = new Thread(task, "PocketPalette client game test " + name);
        thread.setDaemon(true);
        thread.start();
    }
}
