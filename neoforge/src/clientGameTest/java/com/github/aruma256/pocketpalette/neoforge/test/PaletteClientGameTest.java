package com.github.aruma256.pocketpalette.neoforge.test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.lwjgl.glfw.GLFW;

import com.github.aruma256.pocketpalette.PocketPalette;
import com.github.aruma256.pocketpalette.client.gui.PaletteEditScreen;
import com.github.aruma256.pocketpalette.client.palette.ItemPalette;
import com.github.aruma256.pocketpalette.client.palette.ItemState;
import com.github.aruma256.pocketpalette.client.palette.PaletteConfigManager;
import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.AnvilScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.RecipeBookType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;

/** Exercises real keyboard/mouse handling, the mod's event listeners and server SWAP handling. */
final class PaletteClientGameTest {
    static void run(ClientGameTestContext context) {
        ItemStack stone = new ItemStack(Items.STONE, 17);
        stone.set(DataComponents.CUSTOM_NAME, Component.literal("Palette test stone"));
        ItemStack andesite = new ItemStack(Items.ANDESITE, 9);
        context.runOnServer(server -> {
            var player = server.getPlayerList().getPlayers().getFirst();
            player.setGameMode(GameType.SURVIVAL);
            player.getInventory().setSelectedSlot(0);
        });
        setServerInventory(context, Map.of(0, stone, 9, andesite));
        context.waitFor(client -> client.gameMode.getPlayerMode() == GameType.SURVIVAL
                && matchesSlots(client.player.getInventory(), Map.of(0, stone, 9, andesite)));
        context.runOnClient(client -> {
            client.player.getInventory().setSelectedSlot(0);
            ItemPalette.loadGroups(List.of(List.of(List.of(
                    new ItemState(new ItemStack(Items.STONE)),
                    new ItemState(new ItemStack(Items.ANDESITE))))));
        });
        context.holdKey(GLFW.GLFW_KEY_R);
        try {
            context.waitTicks(2);
            context.scroll(1);
            awaitSlots(context, 0, Map.of(0, andesite, 9, stone));
            context.takeScreenshot("palette-after-swap");
            context.scroll(-1);
            awaitSlots(context, 0, Map.of(0, stone, 9, andesite));
        } finally {
            context.releaseKey(GLFW.GLFW_KEY_R);
            context.waitTick();
        }
        // Once R is released, vanilla scrolling must select a different hotbar slot.
        context.scroll(1);
        context.waitFor(client -> client.player.getInventory().getSelectedSlot() == 8);
        testInventoryChangesWhileHeld(context, stone, andesite);
        testEditorBindings(context, stone, andesite);
        testContainerActionKeepsVanilla(context, stone, andesite);
        testTextInputPriority(context, stone, andesite);
        PocketPalette.LOGGER.info("Client game test passed: Survival swap round trip, stack preservation, and R release");
    }

    private static KeyMapping paletteKey(ClientGameTestContext context) {
        return context.computeOnClient(client -> Arrays.stream(client.options.keyMappings)
                .filter(key -> key.getName().equals("key.pocketpalette.palette")).findFirst().orElseThrow());
    }

    private static void testEditorBindings(ClientGameTestContext context, ItemStack stone, ItemStack andesite) {
        // Replace the swap test's immutable fixture with the normal editable config.
        context.runOnClient(client -> {
            if (!PaletteConfigManager.loadAllFromFile()) {
                throw new AssertionError("Editor test config failed to load");
            }
        });
        KeyMapping paletteKey = paletteKey(context);
        InputConstants.Key originalKey = context.computeOnClient(client -> paletteKey.getKey());
        try {
            // Left click also has a container action: opening must consume it.
            for (InputConstants.Key binding : List.of(
                    InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_R),
                    InputConstants.Type.MOUSE.getOrCreate(GLFW.GLFW_MOUSE_BUTTON_4),
                    InputConstants.Type.MOUSE.getOrCreate(GLFW.GLFW_MOUSE_BUTTON_LEFT))) {
                setServerInventory(context, Map.of(0, stone, 9, andesite));
                awaitSlots(context, 0, Map.of(0, stone, 9, andesite));
                bindAndHoverFirstBackpackSlot(context, paletteKey, binding);
                context.pressKey(binding);
                context.waitFor(client -> client.gui.screen() instanceof PaletteEditScreen);
                context.runOnClient(client -> {
                    if (!client.player.inventoryMenu.getCarried().isEmpty()) {
                        throw new AssertionError("Editor input picked up the hovered stack");
                    }
                    client.gui.screen().onClose();
                });
                awaitSlots(context, 0, Map.of(0, stone, 9, andesite));
            }
        } finally {
            context.runOnClient(client -> {
                paletteKey.setKey(originalKey);
                KeyMapping.resetMapping();
                client.gui.setScreen(null);
            });
        }
        // Exercise the editor's MouseButtonEvent and KeyEvent handlers, not just opening it.
        bindAndHoverFirstBackpackSlot(context, paletteKey, originalKey);
        context.pressKey(originalKey);
        context.waitFor(client -> client.gui.screen() instanceof PaletteEditScreen);
        int left = context.computeOnClient(client -> (client.gui.screen().width - 176) / 2);
        int top = context.computeOnClient(client -> Math.max(8, (client.gui.screen().height - 222) / 2));
        // Group 2 is empty in the default config; add the hotbar stone to the andesite seed.
        moveCursor(context, left + 40, top + 22);
        context.pressMouse(GLFW.GLFW_MOUSE_BUTTON_LEFT);
        moveCursor(context, left + 16, top + 189);
        context.pressMouse(GLFW.GLFW_MOUSE_BUTTON_LEFT);
        context.pressKey(GLFW.GLFW_KEY_E);
        context.waitFor(client -> client.gui.screen() == null);
        context.runOnClient(client -> {
            List<ItemState> cycle = ItemPalette.getCycleEntries(new ItemStack(Items.ANDESITE), 1);
            if (cycle == null || cycle.size() != 2
                    || !cycle.get(1).toItemStack().is(Items.STONE)) {
                throw new AssertionError("Editor click did not append the inventory item and save on E");
            }
        });
        awaitSlots(context, 0, Map.of(0, stone, 9, andesite));
        PocketPalette.LOGGER.info("Client game test passed: keyboard and mouse editor bindings consume input");
    }

    private static void moveCursor(ClientGameTestContext context, int guiX, int guiY) {
        double scale = context.computeOnClient(client -> client.getWindow().getGuiScale());
        context.setCursorPos(guiX * scale, guiY * scale);
        context.waitTick();
    }

    /** Unlike Fabric, a key that also has a container action keeps that action on NeoForge. */
    private static void testContainerActionKeepsVanilla(ClientGameTestContext context, ItemStack stone, ItemStack andesite) {
        KeyMapping paletteKey = paletteKey(context);
        InputConstants.Key originalKey = context.computeOnClient(client -> paletteKey.getKey());
        try {
            setServerInventory(context, Map.of(0, stone, 9, andesite));
            awaitSlots(context, 0, Map.of(0, stone, 9, andesite));
            bindAndHoverFirstBackpackSlot(context, paletteKey, InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_Q));
            context.pressKey(GLFW.GLFW_KEY_Q);
            ItemStack oneLess = andesite.copyWithCount(andesite.getCount() - 1);
            awaitSlots(context, 0, Map.of(0, stone, 9, oneLess));
            context.runOnClient(client -> {
                if (client.gui.screen() instanceof PaletteEditScreen) {
                    throw new AssertionError("Editor opened for a key the container handled");
                }
            });
        } finally {
            context.runOnClient(client -> {
                paletteKey.setKey(originalKey);
                KeyMapping.resetMapping();
                client.gui.setScreen(null);
            });
        }
        PocketPalette.LOGGER.info("Client game test passed: container actions keep their keys");
    }

    private static void bindAndHoverFirstBackpackSlot(ClientGameTestContext context, KeyMapping paletteKey, InputConstants.Key binding) {
        double[] cursor = new double[2];
        context.runOnClient(client -> {
            paletteKey.setKey(binding);
            KeyMapping.resetMapping();
            client.gui.setScreen(new InventoryScreen(client.player));
            double scale = client.getWindow().getGuiScale();
            cursor[0] = ((client.gui.screen().width - 176) / 2 + 16) * scale;
            cursor[1] = ((client.gui.screen().height - 166) / 2 + 92) * scale;
        });
        context.setCursorPos(cursor[0], cursor[1]);
        context.waitFor(client -> client.gui.screen() instanceof AbstractContainerScreen<?> screen
                && screen.getHoveredSlot() == client.player.inventoryMenu.getSlot(9));
    }

    /** The editor key must stay with a text field that can take input, and open the editor otherwise. */
    private static void testTextInputPriority(ClientGameTestContext context, ItemStack stone, ItemStack andesite) {
        setServerInventory(context, Map.of(0, stone, 9, andesite));
        awaitSlots(context, 0, Map.of(0, stone, 9, andesite));

        // Recipe book search: focused by the chat key.
        context.runOnClient(client -> client.player.getRecipeBook().setOpen(RecipeBookType.CRAFTING, true));
        openInventoryAndHover(context);
        pressEditorKey(context, true);
        openInventoryAndHover(context);
        context.pressKey(GLFW.GLFW_KEY_T);
        pressEditorKey(context, false);
        context.runOnClient(client -> {
            client.player.getRecipeBook().setOpen(RecipeBookType.CRAFTING, false);
            client.gui.setScreen(null);
        });

        // Anvil name: focused from the start, but editable only with an input item.
        openAnvilAndHover(context);
        pressEditorKey(context, true);
        openAnvilAndHover(context);
        context.runOnServer(server -> {
            var player = server.getPlayerList().getPlayers().getFirst();
            player.containerMenu.getSlot(0).set(stone.copy());
            player.containerMenu.broadcastChanges();
        });
        context.waitFor(client -> client.player.containerMenu.getSlot(0).hasItem());
        pressEditorKey(context, false);
        context.runOnClient(client -> client.player.closeContainer());

        // Creative: only the search tab has a text field, focused by the chat key.
        context.runOnServer(server -> server.getPlayerList().getPlayers().getFirst().setGameMode(GameType.CREATIVE));
        context.waitFor(client -> client.gameMode.getPlayerMode() == GameType.CREATIVE);
        openCreativeAndHover(context);
        pressEditorKey(context, true);
        openCreativeAndHover(context);
        context.pressKey(GLFW.GLFW_KEY_T);
        awaitHoveredItem(context);
        pressEditorKey(context, false);
        context.runOnClient(client -> client.gui.setScreen(null));
        context.runOnServer(server -> server.getPlayerList().getPlayers().getFirst().setGameMode(GameType.SURVIVAL));
        context.waitFor(client -> client.gameMode.getPlayerMode() == GameType.SURVIVAL);
        PocketPalette.LOGGER.info("Client game test passed: text fields keep the editor key");
    }

    private static void openInventoryAndHover(ClientGameTestContext context) {
        hover(context, context.computeOnClient(client -> {
            HoverableInventoryScreen screen = new HoverableInventoryScreen(client.player);
            client.gui.setScreen(screen);
            return screen.firstBackpackSlotCenter();
        }));
    }

    private static void openAnvilAndHover(ClientGameTestContext context) {
        context.runOnServer(server -> server.getPlayerList().getPlayers().getFirst().openMenu(
                new SimpleMenuProvider((id, inventory, player) -> new AnvilMenu(id, inventory), Component.literal("Anvil"))));
        context.waitFor(client -> client.gui.screen() instanceof AnvilScreen);
        hover(context, context.computeOnClient(client -> new int[] {
                (client.gui.screen().width - 176) / 2 + 16, (client.gui.screen().height - 166) / 2 + 92}));
    }

    private static void openCreativeAndHover(ClientGameTestContext context) {
        hover(context, context.computeOnClient(client -> {
            client.gui.setScreen(new CreativeModeInventoryScreen(client.player, client.player.connection.enabledFeatures(), false));
            return new int[] {(client.gui.screen().width - 195) / 2 + 17, (client.gui.screen().height - 136) / 2 + 26};
        }));
    }

    private static void hover(ClientGameTestContext context, int[] guiPos) {
        double scale = context.computeOnClient(client -> client.getWindow().getGuiScale());
        context.setCursorPos(guiPos[0] * scale, guiPos[1] * scale);
        awaitHoveredItem(context);
    }

    private static void awaitHoveredItem(ClientGameTestContext context) {
        context.waitFor(client -> {
            if (!(client.gui.screen() instanceof AbstractContainerScreen<?> screen)) {
                return false;
            }
            Slot slot = screen.getHoveredSlot();
            return slot != null && slot.hasItem();
        });
    }

    private static void pressEditorKey(ClientGameTestContext context, boolean expectEditor) {
        // Let slot changes reach the screen, and the previous key's release clear its state.
        context.waitTicks(2);
        context.pressKey(GLFW.GLFW_KEY_R);
        if (expectEditor) {
            context.waitFor(client -> client.gui.screen() instanceof PaletteEditScreen);
            context.runOnClient(client -> client.gui.screen().onClose());
        } else {
            context.waitTicks(2);
            context.runOnClient(client -> {
                if (client.gui.screen() instanceof PaletteEditScreen) {
                    throw new AssertionError("Editor opened while a text field had focus");
                }
            });
        }
    }

    /** Exposes the slot position, which moves with the recipe book. */
    private static final class HoverableInventoryScreen extends InventoryScreen {
        HoverableInventoryScreen(Player player) {
            super(player);
        }

        int[] firstBackpackSlotCenter() {
            return new int[] {leftPos + 16, topPos + 92};
        }
    }

    private static void testInventoryChangesWhileHeld(ClientGameTestContext context, ItemStack stone, ItemStack andesite) {
        setServerInventory(context, Map.of(0, stone, 9, andesite));
        context.runOnClient(client -> client.player.getInventory().setSelectedSlot(0));
        awaitSlots(context, 0, Map.of(0, stone, 9, andesite));
        context.waitTicks(6); // A fresh press, not a group-switching double tap.
        context.holdKey(GLFW.GLFW_KEY_R);
        try {
            context.waitTicks(2);
            context.holdKey(GLFW.GLFW_KEY_2);
            context.waitTick();
            context.releaseKey(GLFW.GLFW_KEY_2);
            context.waitFor(client -> client.player.getInventory().getSelectedSlot() == 1);
            context.scroll(1);
            context.waitTicks(2);
            // Selecting an empty slot must clear the old ring instead of swapping into it.
            awaitSlots(context, 1, Map.of(0, stone, 9, andesite));

            context.holdKey(GLFW.GLFW_KEY_1);
            context.waitTick();
            context.releaseKey(GLFW.GLFW_KEY_1);
            context.waitFor(client -> client.player.getInventory().getSelectedSlot() == 0);
            context.scroll(1);
            awaitSlots(context, 0, Map.of(0, andesite, 9, stone));

            // Simulate a command removing the candidate and replacing the held item.
            setServerInventory(context, Map.of(0, stone));
            awaitSlots(context, 0, Map.of(0, stone));
            context.scroll(1);
            context.waitTicks(2);
            awaitSlots(context, 0, Map.of(0, stone));

            // A newly populated slot must join the ring even when it was empty at key-down.
            setServerInventory(context, Map.of(0, stone, 10, andesite));
            awaitSlots(context, 0, Map.of(0, stone, 10, andesite));
            context.scroll(1);
            awaitSlots(context, 0, Map.of(0, andesite, 10, stone));

            ItemStack renamedStone = stone.copy();
            renamedStone.setCount(3);
            renamedStone.set(DataComponents.CUSTOM_NAME, Component.literal("Changed by server"));
            setServerInventory(context, Map.of(0, andesite, 10, renamedStone));
            awaitSlots(context, 0, Map.of(0, andesite, 10, renamedStone));
            context.scroll(1);
            awaitSlots(context, 0, Map.of(0, renamedStone, 10, andesite));
            context.scroll(-1);
            awaitSlots(context, 0, Map.of(0, andesite, 10, renamedStone));

            // Own swaps must preserve the ring instead of re-sorting same-item stacks.
            ItemStack smallStone = stone.copy();
            smallStone.setCount(3);
            ItemStack mediumStone = stone.copy();
            mediumStone.setCount(8);
            setServerInventory(context, Map.of(0, stone, 9, smallStone, 10, mediumStone));
            awaitSlots(context, 0, Map.of(0, stone, 9, smallStone, 10, mediumStone));
            context.scroll(1);
            awaitSlots(context, 0, Map.of(0, smallStone, 9, stone, 10, mediumStone));
            context.scroll(1);
            awaitSlots(context, 0, Map.of(0, mediumStone, 9, stone, 10, smallStone));
            context.scroll(1);
            awaitSlots(context, 0, Map.of(0, stone, 9, mediumStone, 10, smallStone));
            PocketPalette.LOGGER.info("Client game test passed: held-key slot changes and server inventory changes");
        } finally {
            context.releaseKey(GLFW.GLFW_KEY_R);
            context.waitTick();
        }
    }

    private static void setServerInventory(ClientGameTestContext context, Map<Integer, ItemStack> stacks) {
        context.runOnServer(server -> {
            var player = server.getPlayerList().getPlayers().getFirst();
            player.getInventory().clearContent();
            stacks.forEach((slot, stack) -> player.getInventory().setItem(slot, stack.copy()));
            player.inventoryMenu.broadcastChanges();
        });
    }

    private static void awaitSlots(ClientGameTestContext context, int selectedSlot, Map<Integer, ItemStack> stacks) {
        context.waitFor(client -> client.player.getInventory().getSelectedSlot() == selectedSlot
                && matchesSlots(client.player.getInventory(), stacks));
        // Check authoritative state too: client prediction alone must not pass this test.
        for (int tick = 0; tick < ClientGameTestContext.DEFAULT_TIMEOUT; tick++) {
            if (context.computeOnServer(server -> {
                var inventory = server.getPlayerList().getPlayers().getFirst().getInventory();
                return inventory.getSelectedSlot() == selectedSlot && matchesSlots(inventory, stacks);
            })) {
                return;
            }
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
}
