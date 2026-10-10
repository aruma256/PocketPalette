package com.github.aruma256.pocketpalette.client;

import java.util.List;
import java.util.Map;

import com.github.aruma256.pocketpalette.PocketPalette;
import com.github.aruma256.pocketpalette.client.gui.PaletteEditScreen;
import com.github.aruma256.pocketpalette.client.mixin.AbstractContainerScreenAccessor;
import com.github.aruma256.pocketpalette.client.palette.ItemPalette;
import com.github.aruma256.pocketpalette.client.palette.ItemState;
import com.github.aruma256.pocketpalette.client.palette.PaletteConfigManager;
import com.mojang.blaze3d.platform.InputConstants;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
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
            world.getConnection().waitForChunksRender();
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
            context.getInput().holdKey(InputConstants.KEY_R);
            try {
                context.waitTicks(2);
                context.getInput().scroll(1);
                awaitInventory(context, world, andesite, stone);
                context.takeScreenshot("palette-after-swap");
                context.getInput().scroll(-1);
                awaitInventory(context, world, stone, andesite);
            } finally {
                context.getInput().releaseKey(InputConstants.KEY_R);
                context.waitTick();
            }
            // Once R is released, vanilla scrolling must select a different hotbar slot.
            context.getInput().scroll(1);
            context.waitFor(client -> client.player.getInventory().getSelectedSlot() == 8);
            testInventoryChangesWhileHeld(context, world, stone, andesite);
            testEditorBindings(context, world, stone, andesite);
            PocketPalette.LOGGER.info("Client game test passed: Survival swap round trip, stack preservation, and R release");
        }
    }

    private static void testEditorBindings(ClientGameTestContext context,
            TestSingleplayerContext world, ItemStack stone, ItemStack andesite) {
        // Replace the swap test's immutable fixture with the normal editable config.
        context.runOnClient(client -> {
            if (!PaletteConfigManager.loadAllFromFile()) {
                throw new AssertionError("Editor test config failed to load");
            }
        });
        KeyMapping paletteKey = context.computeOnClient(client -> java.util.Arrays.stream(client.options.keyMappings)
                .filter(key -> key.getName().equals("key.pocketpalette.palette")).findFirst().orElseThrow());
        InputConstants.Key originalKey = context.computeOnClient(client -> KeyMappingHelper.getBoundKeyOf(paletteKey));
        try {
            // Q and left click also have container actions: opening must consume them.
            for (InputConstants.Key binding : List.of(
                    InputConstants.Type.KEYBOARD.getOrCreate(InputConstants.KEY_R),
                    InputConstants.Type.MOUSE.getOrCreate(InputConstants.MOUSE_BUTTON_4),
                    InputConstants.Type.MOUSE.getOrCreate(InputConstants.MOUSE_BUTTON_LEFT),
                    InputConstants.Type.KEYBOARD.getOrCreate(InputConstants.KEY_Q))) {
                setServerInventory(world, Map.of(0, stone, 9, andesite));
                awaitSlots(context, world, 0, Map.of(0, stone, 9, andesite));
                double[] cursor = new double[2];
                context.runOnClient(client -> {
                    paletteKey.setKey(binding);
                    KeyMapping.resetMapping();
                    client.gui.setScreen(new InventoryScreen(client.player));
                    double scale = client.getWindow().getGuiScale();
                    cursor[0] = ((client.gui.screen().width - 176) / 2 + 16) * scale;
                    cursor[1] = ((client.gui.screen().height - 166) / 2 + 92) * scale;
                });
                context.getInput().setCursorPos(cursor[0], cursor[1]);
                context.waitFor(client -> ((AbstractContainerScreenAccessor) client.gui.screen())
                        .pocketpalette$getHoveredSlot() == client.player.inventoryMenu.getSlot(9));
                context.getInput().pressKey(binding);
                context.waitFor(client -> client.gui.screen() instanceof PaletteEditScreen);
                context.runOnClient(client -> {
                    if (!client.player.inventoryMenu.getCarried().isEmpty()) {
                        throw new AssertionError("Editor input picked up the hovered stack");
                    }
                    client.gui.screen().onClose();
                });
                awaitSlots(context, world, 0, Map.of(0, stone, 9, andesite));
            }
        } finally {
            context.runOnClient(client -> {
                paletteKey.setKey(originalKey);
                KeyMapping.resetMapping();
                client.gui.setScreen(null);
            });
        }
        // Exercise the editor's MouseButtonEvent and KeyEvent handlers, not just opening it.
        context.runOnClient(client -> client.gui.setScreen(new InventoryScreen(client.player)));
        moveCursor(context, (context.computeOnClient(client -> client.gui.screen().width) - 176) / 2 + 16,
                (context.computeOnClient(client -> client.gui.screen().height) - 166) / 2 + 92);
        context.waitFor(client -> ((AbstractContainerScreenAccessor) client.gui.screen())
                .pocketpalette$getHoveredSlot() == client.player.inventoryMenu.getSlot(9));
        context.getInput().pressKey(originalKey);
        context.waitFor(client -> client.gui.screen() instanceof PaletteEditScreen);
        int left = context.computeOnClient(client -> (client.gui.screen().width - 176) / 2);
        int top = context.computeOnClient(client -> Math.max(8, (client.gui.screen().height - 222) / 2));
        // Group 2 is empty in the default config; add the hotbar stone to the andesite seed.
        moveCursor(context, left + 40, top + 22);
        context.getInput().pressMouse(InputConstants.MOUSE_BUTTON_LEFT);
        moveCursor(context, left + 16, top + 189);
        context.getInput().pressMouse(InputConstants.MOUSE_BUTTON_LEFT);
        context.getInput().pressKey(InputConstants.KEY_E);
        context.waitFor(client -> client.gui.screen() == null);
        context.runOnClient(client -> {
            List<ItemState> cycle = ItemPalette.getCycleEntries(new ItemStack(Items.ANDESITE), 1);
            if (cycle == null || cycle.size() != 2
                    || !cycle.get(1).toItemStack().is(Items.STONE)) {
                throw new AssertionError("Editor click did not append the inventory item and save on E");
            }
        });
        awaitSlots(context, world, 0, Map.of(0, stone, 9, andesite));
        PocketPalette.LOGGER.info("Client game test passed: keyboard and mouse editor bindings consume input");
    }

    private static void moveCursor(ClientGameTestContext context, int guiX, int guiY) {
        double scale = context.computeOnClient(client -> client.getWindow().getGuiScale());
        context.getInput().setCursorPos(guiX * scale, guiY * scale);
        context.waitTick();
    }

    private static void testInventoryChangesWhileHeld(ClientGameTestContext context,
            TestSingleplayerContext world, ItemStack stone, ItemStack andesite) {
        setServerInventory(world, Map.of(0, stone, 9, andesite));
        context.runOnClient(client -> client.player.getInventory().setSelectedSlot(0));
        awaitSlots(context, world, 0, Map.of(0, stone, 9, andesite));
        context.waitTicks(6); // A fresh press, not a group-switching double tap.
        context.getInput().holdKey(InputConstants.KEY_R);
        try {
            context.waitTicks(2);
            context.getInput().holdKey(InputConstants.KEY_2);
            context.waitTick();
            context.getInput().releaseKey(InputConstants.KEY_2);
            context.waitFor(client -> client.player.getInventory().getSelectedSlot() == 1);
            context.getInput().scroll(1);
            context.waitTicks(2);
            // Selecting an empty slot must clear the old ring instead of swapping into it.
            awaitSlots(context, world, 1, Map.of(0, stone, 9, andesite));

            context.getInput().holdKey(InputConstants.KEY_1);
            context.waitTick();
            context.getInput().releaseKey(InputConstants.KEY_1);
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
            context.getInput().releaseKey(InputConstants.KEY_R);
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
