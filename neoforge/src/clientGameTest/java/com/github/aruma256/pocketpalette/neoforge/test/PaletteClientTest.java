package com.github.aruma256.pocketpalette.neoforge.test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.lwjgl.glfw.GLFW;

import com.github.aruma256.pocketpalette.client.gui.PaletteEditScreen;
import com.github.aruma256.pocketpalette.client.palette.ItemPalette;
import com.github.aruma256.pocketpalette.client.palette.ItemState;
import com.github.aruma256.pocketpalette.client.palette.PaletteConfigManager;
import com.github.aruma256.pocketpalette.neoforge.test.ClientTestMod.Ctx;
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

/** Port of fabric's PaletteClientGameTest onto {@link Ctx}. */
final class PaletteClientTest {
    static final List<String> PASSED = new ArrayList<>();

    static void run(Ctx context) {
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
            context.scroll(-1);
            awaitSlots(context, 0, Map.of(0, stone, 9, andesite));
        } finally {
            context.releaseKey(GLFW.GLFW_KEY_R);
            context.waitTick();
        }
        // Once R is released, vanilla scrolling must select a different hotbar slot.
        context.scroll(1);
        context.waitFor(client -> client.player.getInventory().getSelectedSlot() == 8);
        PASSED.add("swap round trip / vanilla scroll after release");

        testInventoryChangesWhileHeld(context, stone, andesite);
        PASSED.add("held-key slot changes and server inventory changes");
        testEditorBindings(context, stone, andesite);
        PASSED.add("editor opens from R / mouse 4 / left click without picking up the stack");
        testContainerActionKeepsVanilla(context, stone, andesite);
        PASSED.add("NeoForge: Q bound to the editor keeps the vanilla drop");
        testTextInputPriority(context, stone, andesite);
        PASSED.add("text fields keep the editor key (recipe book / anvil / creative)");
    }

    private static KeyMapping paletteKey(Ctx context) {
        return context.computeOnClient(client -> Arrays.stream(client.options.keyMappings)
                .filter(key -> key.getName().equals("key.pocketpalette.palette")).findFirst().orElseThrow());
    }

    private static void testEditorBindings(Ctx context, ItemStack stone, ItemStack andesite) {
        context.runOnClient(client -> {
            if (!PaletteConfigManager.loadAllFromFile()) {
                throw new AssertionError("Editor test config failed to load");
            }
        });
        KeyMapping paletteKey = paletteKey(context);
        InputConstants.Key originalKey = context.computeOnClient(client -> paletteKey.getKey());
        try {
            for (InputConstants.Key binding : List.of(
                    InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_R),
                    InputConstants.Type.MOUSE.getOrCreate(GLFW.GLFW_MOUSE_BUTTON_4),
                    InputConstants.Type.MOUSE.getOrCreate(GLFW.GLFW_MOUSE_BUTTON_LEFT))) {
                setServerInventory(context, Map.of(0, stone, 9, andesite));
                awaitSlots(context, 0, Map.of(0, stone, 9, andesite));
                bindAndHoverFirstBackpackSlot(context, paletteKey, binding);
                context.pressKey(binding);
                context.waitFor(client -> client.screen instanceof PaletteEditScreen);
                context.runOnClient(client -> {
                    if (!client.player.inventoryMenu.getCarried().isEmpty()) {
                        throw new AssertionError("Editor input picked up the hovered stack");
                    }
                    client.screen.onClose();
                });
                awaitSlots(context, 0, Map.of(0, stone, 9, andesite));
            }
        } finally {
            context.runOnClient(client -> {
                paletteKey.setKey(originalKey);
                KeyMapping.resetMapping();
                client.setScreen(null);
            });
        }
    }

    /** Documented loader difference: on NeoForge a key with a container action keeps that action. */
    private static void testContainerActionKeepsVanilla(Ctx context, ItemStack stone, ItemStack andesite) {
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
                if (client.screen instanceof PaletteEditScreen) {
                    throw new AssertionError("Editor opened for a key the container handled");
                }
            });
        } finally {
            context.runOnClient(client -> {
                paletteKey.setKey(originalKey);
                KeyMapping.resetMapping();
                client.setScreen(null);
            });
        }
    }

    private static void bindAndHoverFirstBackpackSlot(Ctx context, KeyMapping paletteKey, InputConstants.Key binding) {
        double[] cursor = new double[2];
        context.runOnClient(client -> {
            paletteKey.setKey(binding);
            KeyMapping.resetMapping();
            client.setScreen(new InventoryScreen(client.player));
            double scale = client.getWindow().getGuiScale();
            cursor[0] = ((client.screen.width - 176) / 2 + 16) * scale;
            cursor[1] = ((client.screen.height - 166) / 2 + 92) * scale;
        });
        context.setCursorPos(cursor[0], cursor[1]);
        context.waitFor(client -> client.screen instanceof AbstractContainerScreen<?> screen
                && screen.getSlotUnderMouse() == client.player.inventoryMenu.getSlot(9));
    }

    private static void testTextInputPriority(Ctx context, ItemStack stone, ItemStack andesite) {
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
            client.setScreen(null);
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
        context.runOnClient(client -> client.setScreen(null));
        context.runOnServer(server -> server.getPlayerList().getPlayers().getFirst().setGameMode(GameType.SURVIVAL));
        context.waitFor(client -> client.gameMode.getPlayerMode() == GameType.SURVIVAL);
    }

    private static void openInventoryAndHover(Ctx context) {
        hover(context, context.computeOnClient(client -> {
            HoverableInventoryScreen screen = new HoverableInventoryScreen(client.player);
            client.setScreen(screen);
            return screen.firstBackpackSlotCenter();
        }));
    }

    private static void openAnvilAndHover(Ctx context) {
        context.runOnServer(server -> server.getPlayerList().getPlayers().getFirst().openMenu(
                new SimpleMenuProvider((id, inventory, player) -> new AnvilMenu(id, inventory), Component.literal("Anvil"))));
        context.waitFor(client -> client.screen instanceof AnvilScreen);
        hover(context, context.computeOnClient(client -> new int[] {
                (client.screen.width - 176) / 2 + 16, (client.screen.height - 166) / 2 + 92}));
    }

    private static void openCreativeAndHover(Ctx context) {
        hover(context, context.computeOnClient(client -> {
            client.setScreen(new CreativeModeInventoryScreen(client.player, client.player.connection.enabledFeatures(), false));
            return new int[] {(client.screen.width - 195) / 2 + 17, (client.screen.height - 136) / 2 + 26};
        }));
    }

    private static void hover(Ctx context, int[] guiPos) {
        double scale = context.computeOnClient(client -> client.getWindow().getGuiScale());
        context.setCursorPos(guiPos[0] * scale, guiPos[1] * scale);
        awaitHoveredItem(context);
    }

    private static void awaitHoveredItem(Ctx context) {
        context.waitFor(client -> {
            if (!(client.screen instanceof AbstractContainerScreen<?> screen)) {
                return false;
            }
            Slot slot = screen.getSlotUnderMouse();
            return slot != null && slot.hasItem();
        });
    }

    private static void pressEditorKey(Ctx context, boolean expectEditor) {
        context.waitTicks(2);
        context.pressKey(GLFW.GLFW_KEY_R);
        if (expectEditor) {
            context.waitFor(client -> client.screen instanceof PaletteEditScreen);
            context.runOnClient(client -> client.screen.onClose());
        } else {
            context.waitTicks(2);
            context.runOnClient(client -> {
                if (client.screen instanceof PaletteEditScreen) {
                    throw new AssertionError("Editor opened while a text field had focus");
                }
            });
        }
    }

    private static final class HoverableInventoryScreen extends InventoryScreen {
        HoverableInventoryScreen(Player player) {
            super(player);
        }

        int[] firstBackpackSlotCenter() {
            return new int[] {leftPos + 16, topPos + 92};
        }
    }

    private static void testInventoryChangesWhileHeld(Ctx context, ItemStack stone, ItemStack andesite) {
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
        } finally {
            context.releaseKey(GLFW.GLFW_KEY_R);
            context.waitTick();
        }
    }

    private static void setServerInventory(Ctx context, Map<Integer, ItemStack> stacks) {
        context.runOnServer(server -> {
            var player = server.getPlayerList().getPlayers().getFirst();
            player.getInventory().clearContent();
            stacks.forEach((slot, stack) -> player.getInventory().setItem(slot, stack.copy()));
            player.inventoryMenu.broadcastChanges();
        });
    }

    private static void awaitSlots(Ctx context, int selectedSlot, Map<Integer, ItemStack> stacks) {
        context.waitFor(client -> client.player.getInventory().getSelectedSlot() == selectedSlot
                && matchesSlots(client.player.getInventory(), stacks));
        // Check authoritative state too: client prediction alone must not pass this test.
        for (int tick = 0; tick < ClientTestMod.TIMEOUT_TICKS; tick++) {
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

    private PaletteClientTest() {
    }
}
