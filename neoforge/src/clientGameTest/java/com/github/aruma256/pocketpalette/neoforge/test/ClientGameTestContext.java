package com.github.aruma256.pocketpalette.neoforge.test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import org.lwjgl.sdl.SDLEvents;
import org.lwjgl.sdl.SDLKeyboard;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.Screenshot;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;

/**
 * Drives the client from the test thread, standing in for Fabric's ClientGameTestContext.
 * Unlike Fabric's, the game keeps running between calls, so tests must wait for results.
 */
final class ClientGameTestContext {
    static final int DEFAULT_TIMEOUT = 200;
    private static final int WORLD_TIMEOUT = 1200;
    private static final long TASK_TIMEOUT_SECONDS = 60;
    private static final int[] REAL_INPUT_EVENTS = {
            SDLEvents.SDL_EVENT_KEY_DOWN, SDLEvents.SDL_EVENT_KEY_UP,
            SDLEvents.SDL_EVENT_TEXT_EDITING, SDLEvents.SDL_EVENT_TEXT_INPUT,
            SDLEvents.SDL_EVENT_MOUSE_MOTION,
            SDLEvents.SDL_EVENT_MOUSE_BUTTON_DOWN, SDLEvents.SDL_EVENT_MOUSE_BUTTON_UP,
            SDLEvents.SDL_EVENT_MOUSE_WHEEL};

    private final AtomicInteger ticks = new AtomicInteger();

    void onTick() {
        ticks.incrementAndGet();
    }

    /** Someone using the machine must not move the cursor or press keys during a local run. */
    void blockRealInput() {
        runOnClient(client -> {
            for (int event : REAL_INPUT_EVENTS) {
                SDLEvents.SDL_SetEventEnabled(event, false);
            }
        });
    }

    void createWorld() {
        runOnClient(client -> client.createWorldOpenFlows().createFreshLevel(
                "client-game-test",
                new LevelSettings("client-game-test", GameType.SURVIVAL,
                        new LevelSettings.DifficultySettings(Difficulty.PEACEFUL, false, false), true,
                        WorldDataConfiguration.DEFAULT),
                WorldOptions.defaultWithRandomSeed(),
                WorldPresets::createTestWorldDimensions,
                client.gui.screen()));
        waitFor(client -> client.player != null && client.level != null && client.gui.screen() == null, WORLD_TIMEOUT);
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
        return await(future);
    }

    void runOnClient(Consumer<Minecraft> task) {
        computeOnClient(client -> {
            task.accept(client);
            return null;
        });
    }

    <T> T computeOnServer(Function<MinecraftServer, T> task) {
        MinecraftServer server = computeOnClient(Minecraft::getSingleplayerServer);
        return await(CompletableFuture.supplyAsync(() -> task.apply(server), server));
    }

    void runOnServer(Consumer<MinecraftServer> task) {
        computeOnServer(server -> {
            task.accept(server);
            return null;
        });
    }

    private static <T> T await(CompletableFuture<T> future) {
        try {
            return future.get(TASK_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (Exception e) {
            throw new AssertionError("Task on the game thread failed", e);
        }
    }

    void waitTick() {
        waitTicks(1);
    }

    void waitTicks(int count) {
        int target = ticks.get() + count;
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(TASK_TIMEOUT_SECONDS);
        while (ticks.get() < target) {
            if (System.nanoTime() > deadline) {
                throw new AssertionError("Client stopped ticking");
            }
            pause();
        }
    }

    void waitFor(Predicate<Minecraft> condition) {
        waitFor(condition, DEFAULT_TIMEOUT);
    }

    private void waitFor(Predicate<Minecraft> condition, int timeoutTicks) {
        int deadline = ticks.get() + timeoutTicks;
        while (!computeOnClient(condition::test)) {
            if (ticks.get() >= deadline) {
                throw new AssertionError("Condition not met within " + timeoutTicks + " ticks");
            }
            pause();
        }
    }

    private static void pause() {
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            throw new AssertionError(e);
        }
    }

    void takeScreenshot(String name) {
        runOnClient(client -> Screenshot.grab(client.gameDirectory, name + ".png", client.gameRenderer.mainRenderTarget(), 1, message -> {}));
    }

    void holdKey(int key) {
        sendInput(InputConstants.Type.KEYBOARD.getOrCreate(key), InputConstants.PRESS);
    }

    void releaseKey(int key) {
        sendInput(InputConstants.Type.KEYBOARD.getOrCreate(key), InputConstants.RELEASE);
    }

    void pressKey(int key) {
        pressKey(InputConstants.Type.KEYBOARD.getOrCreate(key));
    }

    void pressKey(InputConstants.Key key) {
        sendInput(key, InputConstants.PRESS);
        waitTick();
        sendInput(key, InputConstants.RELEASE);
    }

    void pressMouse(int button) {
        pressKey(InputConstants.Type.MOUSE.getOrCreate(button));
    }

    private void sendInput(InputConstants.Key key, int action) {
        runOnClient(client -> {
            long window = client.getWindow().handle();
            if (key.getType() == InputConstants.Type.MOUSE) {
                client.mouseHandler.onButton(window, new MouseButtonInfo(key.getValue(), 0), action);
            } else {
                client.keyboardHandler.keyPress(window, action,
                        new KeyEvent(key.getValue(), SDLKeyboard.SDL_GetKeyFromScancode(key.getValue(), (short) 0, true), 0));
            }
        });
    }

    void scroll(double amount) {
        runOnClient(client -> client.mouseHandler.onScroll(client.getWindow().handle(), 0.0, amount));
    }

    void setCursorPos(double x, double y) {
        runOnClient(client -> client.mouseHandler.onMove(client.getWindow().handle(), x, y, 0.0, 0.0));
    }
}
