package com.github.aruma256.pocketpalette.neoforge.test;

import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import org.lwjgl.glfw.GLFW;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;

/** Drives the client from a separate thread, in place of Fabric's FabricClientGameTest harness. */
@Mod(value = "pocketpalette_test", dist = Dist.CLIENT)
public final class ClientTestMod {
    static final int TIMEOUT_TICKS = 200;
    private static final AtomicInteger TICKS = new AtomicInteger();

    public ClientTestMod() {
        Thread watchdog = new Thread(() -> {
            try {
                Thread.sleep(600_000);
            } catch (InterruptedException e) {
                return;
            }
            System.out.println("[client-test] WATCHDOG: timed out, screen=" + Minecraft.getInstance().screen);
            System.out.flush();
            Runtime.getRuntime().halt(2);
        }, "PocketPalette client test watchdog");
        watchdog.setDaemon(true);
        watchdog.start();
        AtomicBoolean started = new AtomicBoolean();
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post event) -> {
            TICKS.incrementAndGet();
            if (Minecraft.getInstance().screen instanceof TitleScreen && started.compareAndSet(false, true)) {
                Thread thread = new Thread(ClientTestMod::runAll, "PocketPalette client test");
                thread.setDaemon(true);
                thread.start();
            }
        });
    }

    private static void runAll() {
        int exit = 1;
        String result;
        try {
            Ctx ctx = new Ctx();
            ctx.createWorld();
            PaletteClientTest.run(ctx);
            exit = 0;
            result = "PASS";
        } catch (Throwable t) {
            t.printStackTrace();
            result = "FAIL: " + t;
        }
        try {
            Files.writeString(Path.of("client-test-result.txt"), result + "\n" + String.join("\n", PaletteClientTest.PASSED) + "\n");
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("[client-test] " + result + " " + PaletteClientTest.PASSED);
        System.out.flush();
        if (exit == 0) {
            // A normal shutdown, because halting under a live GLFW window exits with 0xC0000409 on Windows.
            Minecraft client = Minecraft.getInstance();
            client.execute(client::stop);
        } else {
            Runtime.getRuntime().halt(exit);
        }
    }

    /** Same shape as Fabric's ClientGameTestContext, so tests can be shared in spirit. */
    static final class Ctx {
        private final Method onScroll;
        private final Method onPress;
        private final Method onMove;

        Ctx() throws Exception {
            onScroll = accessible("onScroll", long.class, double.class, double.class);
            onPress = accessible("onPress", long.class, int.class, int.class, int.class);
            onMove = accessible("onMove", long.class, double.class, double.class);
        }

        private static Method accessible(String name, Class<?>... types) throws Exception {
            Method method = MouseHandler.class.getDeclaredMethod(name, types);
            method.setAccessible(true);
            return method;
        }

        void createWorld() {
            runOnClient(client -> client.createWorldOpenFlows().createFreshLevel(
                    "client-test-" + System.currentTimeMillis(),
                    new LevelSettings("client-test", GameType.SURVIVAL, false, Difficulty.PEACEFUL, true, new GameRules(WorldDataConfiguration.DEFAULT.enabledFeatures()), WorldDataConfiguration.DEFAULT),
                    WorldOptions.defaultWithRandomSeed(),
                    WorldPresets::createFlatWorldDimensions,
                    client.screen));
            waitFor(client -> client.player != null && client.level != null && client.screen == null, 1200);
            waitTicks(10);
        }

        <T> T computeOnClient(Function<Minecraft, T> task) {
            Minecraft client = Minecraft.getInstance();
            CompletableFuture<T> future = new CompletableFuture<>();
            client.execute(() -> {
                try {
                    future.complete(task.apply(client));
                } catch (Throwable t) {
                    future.completeExceptionally(t);
                }
            });
            try {
                return future.get(60, TimeUnit.SECONDS);
            } catch (Exception e) {
                throw new AssertionError("Client task failed", e);
            }
        }

        void runOnClient(Consumer<Minecraft> task) {
            computeOnClient(client -> {
                task.accept(client);
                return 0;
            });
        }

        <T> T computeOnServer(Function<MinecraftServer, T> task) {
            MinecraftServer server = computeOnClient(Minecraft::getSingleplayerServer);
            try {
                return CompletableFuture.supplyAsync(() -> task.apply(server), server).get(60, TimeUnit.SECONDS);
            } catch (Exception e) {
                throw new AssertionError("Server task failed", e);
            }
        }

        void runOnServer(Consumer<MinecraftServer> task) {
            computeOnServer(server -> {
                task.accept(server);
                return 0;
            });
        }

        void waitTick() {
            waitTicks(1);
        }

        void waitTicks(int ticks) {
            int target = TICKS.get() + ticks;
            sleepUntil(() -> TICKS.get() >= target, "ticks");
        }

        void waitFor(Predicate<Minecraft> condition) {
            waitFor(condition, TIMEOUT_TICKS);
        }

        void waitFor(Predicate<Minecraft> condition, int timeoutTicks) {
            int deadline = TICKS.get() + timeoutTicks;
            while (TICKS.get() < deadline) {
                if (computeOnClient(condition::test)) {
                    return;
                }
                sleep();
            }
            throw new AssertionError("Condition not met within " + timeoutTicks + " ticks");
        }

        private void sleepUntil(java.util.function.BooleanSupplier condition, String what) {
            long deadline = System.currentTimeMillis() + 60_000;
            while (!condition.getAsBoolean()) {
                if (System.currentTimeMillis() > deadline) {
                    throw new AssertionError("Timed out waiting for " + what);
                }
                sleep();
            }
        }

        private static void sleep() {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                throw new AssertionError(e);
            }
        }

        void holdKey(int key) {
            sendKey(InputConstants.Type.KEYSYM.getOrCreate(key), GLFW.GLFW_PRESS);
        }

        void releaseKey(int key) {
            sendKey(InputConstants.Type.KEYSYM.getOrCreate(key), GLFW.GLFW_RELEASE);
        }

        void pressKey(int key) {
            pressKey(InputConstants.Type.KEYSYM.getOrCreate(key));
        }

        void pressKey(InputConstants.Key key) {
            sendKey(key, GLFW.GLFW_PRESS);
            waitTick();
            sendKey(key, GLFW.GLFW_RELEASE);
        }

        private void sendKey(InputConstants.Key key, int action) {
            runOnClient(client -> {
                long window = client.getWindow().getWindow();
                try {
                    if (key.getType() == InputConstants.Type.MOUSE) {
                        onPress.invoke(client.mouseHandler, window, key.getValue(), action, 0);
                    } else {
                        client.keyboardHandler.keyPress(window, key.getValue(), GLFW.glfwGetKeyScancode(key.getValue()), action, 0);
                    }
                } catch (ReflectiveOperationException e) {
                    throw new AssertionError(e);
                }
            });
        }

        void scroll(double amount) {
            runOnClient(client -> {
                try {
                    onScroll.invoke(client.mouseHandler, client.getWindow().getWindow(), 0.0, amount);
                } catch (ReflectiveOperationException e) {
                    throw new AssertionError(e);
                }
            });
        }

        void setCursorPos(double x, double y) {
            runOnClient(client -> {
                try {
                    onMove.invoke(client.mouseHandler, client.getWindow().getWindow(), x, y);
                } catch (ReflectiveOperationException e) {
                    throw new AssertionError(e);
                }
            });
        }
    }
}
