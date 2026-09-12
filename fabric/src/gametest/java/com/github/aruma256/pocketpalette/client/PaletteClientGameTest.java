package com.github.aruma256.pocketpalette.client;

import java.util.List;

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
            PocketPalette.LOGGER.info("Client game test passed: Survival swap round trip, stack preservation, and R release");
        }
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
