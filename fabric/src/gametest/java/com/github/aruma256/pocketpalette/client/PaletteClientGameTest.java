package com.github.aruma256.pocketpalette.client;

import java.util.List;
import java.util.Map;

import org.lwjgl.glfw.GLFW;

import com.github.aruma256.pocketpalette.PocketPalette;
import com.github.aruma256.pocketpalette.client.palette.ItemPalette;
import com.github.aruma256.pocketpalette.client.palette.ItemState;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;

/** Exercises real keyboard/mouse callbacks, the mod's mixins and server SWAP handling. */
public class PaletteClientGameTest implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext context) {
        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            world.getClientWorld().waitForChunksRender();
            ItemStack stone = new ItemStack(Items.STONE, 17);
            stone.set(DataComponents.CUSTOM_NAME, Component.literal("Palette test stone"));
            ItemStack andesite = new ItemStack(Items.ANDESITE, 9);
            world.getServer().runOnServer(server -> {
                var player = server.getPlayerList().getPlayers().getFirst();
                player.setGameMode(GameType.SURVIVAL);
                player.getInventory().clearContent();
                player.getInventory().setSelectedSlot(0);
                player.getInventory().setItem(0, stone.copy());
                player.getInventory().setItem(9, andesite.copy());
                player.inventoryMenu.broadcastChanges();
            });
            context.waitFor(client -> client.gameMode.getPlayerMode() == GameType.SURVIVAL
                    && matches(client.player.getInventory(), stone, andesite));
            context.runOnClient(client -> {
                client.player.getInventory().setSelectedSlot(0);
                ItemPalette.loadGroups(List.of(List.of(List.of(
                        new ItemState(new ItemStack(Items.STONE)),
                        new ItemState(new ItemStack(Items.ANDESITE))))));
            });
            context.getInput().holdKey(GLFW.GLFW_KEY_R);
            try {
                context.waitTicks(2);
                context.getInput().scroll(1);
                awaitInventory(context, world, andesite, stone);
                context.takeScreenshot("palette-after-swap");
                context.getInput().scroll(-1);
                awaitInventory(context, world, stone, andesite);
            } finally {
                context.getInput().releaseKey(GLFW.GLFW_KEY_R);
                context.waitTick();
            }
            // Once R is released, vanilla scrolling must select a different hotbar slot.
            context.getInput().scroll(1);
            context.waitFor(client -> client.player.getInventory().getSelectedSlot() == 8);
            testInventoryChangesWhileHeld(context, world, stone, andesite);
            PocketPalette.LOGGER.info("Client game test passed: Survival swap round trip, stack preservation, and R release");
        }
    }

    private static void testInventoryChangesWhileHeld(ClientGameTestContext context,
            TestSingleplayerContext world, ItemStack stone, ItemStack andesite) {
        setServerInventory(world, Map.of(0, stone, 9, andesite));
        context.runOnClient(client -> client.player.getInventory().setSelectedSlot(0));
        awaitSlots(context, world, 0, Map.of(0, stone, 9, andesite));
        context.waitTicks(6); // A fresh press, not a group-switching double tap.
        context.getInput().holdKey(GLFW.GLFW_KEY_R);
        try {
            context.waitTicks(2);
            context.getInput().holdKey(GLFW.GLFW_KEY_2);
            context.waitTick();
            context.getInput().releaseKey(GLFW.GLFW_KEY_2);
            context.waitFor(client -> client.player.getInventory().getSelectedSlot() == 1);
            context.getInput().scroll(1);
            context.waitTicks(2);
            // Selecting an empty slot must clear the old ring instead of swapping into it.
            awaitSlots(context, world, 1, Map.of(0, stone, 9, andesite));

            context.getInput().holdKey(GLFW.GLFW_KEY_1);
            context.waitTick();
            context.getInput().releaseKey(GLFW.GLFW_KEY_1);
            context.waitFor(client -> client.player.getInventory().getSelectedSlot() == 0);
            context.getInput().scroll(1);
            awaitSlots(context, world, 0, Map.of(0, andesite, 9, stone));

            // Simulate a command removing the candidate and replacing the held item.
            setServerInventory(world, Map.of(0, stone));
            awaitSlots(context, world, 0, Map.of(0, stone));
            context.getInput().scroll(1);
            context.waitTicks(2);
            awaitSlots(context, world, 0, Map.of(0, stone));

            // A newly populated slot must join the ring even when it was empty at key-down.
            setServerInventory(world, Map.of(0, stone, 10, andesite));
            awaitSlots(context, world, 0, Map.of(0, stone, 10, andesite));
            context.getInput().scroll(1);
            awaitSlots(context, world, 0, Map.of(0, andesite, 10, stone));

            ItemStack renamedStone = stone.copy();
            renamedStone.setCount(3);
            renamedStone.set(DataComponents.CUSTOM_NAME, Component.literal("Changed by server"));
            setServerInventory(world, Map.of(0, andesite, 10, renamedStone));
            awaitSlots(context, world, 0, Map.of(0, andesite, 10, renamedStone));
            context.getInput().scroll(1);
            awaitSlots(context, world, 0, Map.of(0, renamedStone, 10, andesite));
            context.getInput().scroll(-1);
            awaitSlots(context, world, 0, Map.of(0, andesite, 10, renamedStone));

            // Own swaps must preserve the ring instead of re-sorting same-item stacks.
            ItemStack smallStone = stone.copy();
            smallStone.setCount(3);
            ItemStack mediumStone = stone.copy();
            mediumStone.setCount(8);
            setServerInventory(world, Map.of(0, stone, 9, smallStone, 10, mediumStone));
            awaitSlots(context, world, 0, Map.of(0, stone, 9, smallStone, 10, mediumStone));
            context.getInput().scroll(1);
            awaitSlots(context, world, 0, Map.of(0, smallStone, 9, stone, 10, mediumStone));
            context.getInput().scroll(1);
            awaitSlots(context, world, 0, Map.of(0, mediumStone, 9, stone, 10, smallStone));
            context.getInput().scroll(1);
            awaitSlots(context, world, 0, Map.of(0, stone, 9, mediumStone, 10, smallStone));
            PocketPalette.LOGGER.info("Client game test passed: held-key slot changes and server inventory changes");
        } finally {
            context.getInput().releaseKey(GLFW.GLFW_KEY_R);
            context.waitTick();
        }
    }

    private static void setServerInventory(TestSingleplayerContext world, Map<Integer, ItemStack> stacks) {
        world.getServer().runOnServer(server -> {
            var player = server.getPlayerList().getPlayers().getFirst();
            player.getInventory().clearContent();
            stacks.forEach((slot, stack) -> player.getInventory().setItem(slot, stack.copy()));
            player.inventoryMenu.broadcastChanges();
        });
    }

    private static void awaitSlots(ClientGameTestContext context, TestSingleplayerContext world,
            int selectedSlot, Map<Integer, ItemStack> stacks) {
        context.waitFor(client -> client.player.getInventory().getSelectedSlot() == selectedSlot
                && matchesSlots(client.player.getInventory(), stacks));
        for (int tick = 0; tick < ClientGameTestContext.DEFAULT_TIMEOUT; tick++) {
            if (world.getServer().computeOnServer(server -> {
                var inventory = server.getPlayerList().getPlayers().getFirst().getInventory();
                return inventory.getSelectedSlot() == selectedSlot && matchesSlots(inventory, stacks);
            })) return;
            context.waitTick();
        }
        throw new AssertionError("Server inventory did not confirm expected slots: " + stacks.keySet());
    }

    private static boolean matchesSlots(Inventory inventory, Map<Integer, ItemStack> stacks) {
        for (int slot = 0; slot < Inventory.INVENTORY_SIZE; slot++) {
            if (!ItemStack.matches(inventory.getItem(slot), stacks.getOrDefault(slot, ItemStack.EMPTY))) {
                return false;
            }
        }
        return true;
    }

    private static void awaitInventory(ClientGameTestContext context, TestSingleplayerContext world,
            ItemStack hand, ItemStack source) {
        context.waitFor(client -> matches(client.player.getInventory(), hand, source)
                && client.player.getInventory().getSelectedSlot() == 0);
        // Check authoritative state too: client prediction alone must not pass this test.
        for (int tick = 0; tick < ClientGameTestContext.DEFAULT_TIMEOUT; tick++) {
            if (world.getServer().computeOnServer(server -> {
                var inventory = server.getPlayerList().getPlayers().getFirst().getInventory();
                return matches(inventory, hand, source) && inventory.getSelectedSlot() == 0;
            })) {
                return;
            }
            context.waitTick();
        }
        throw new AssertionError("Server inventory did not confirm the palette swap");
    }

    private static boolean matches(Inventory inventory, ItemStack hand, ItemStack source) {
        for (int slot = 0; slot < Inventory.INVENTORY_SIZE; slot++) {
            ItemStack expected = slot == 0 ? hand : slot == 9 ? source : ItemStack.EMPTY;
            if (!ItemStack.matches(inventory.getItem(slot), expected)) {
                return false;
            }
        }
        return true;
    }
}
