package com.github.aruma256.pocketpalette.client.keybinding;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;

import com.github.aruma256.pocketpalette.MinecraftBootstrapExtension;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

@ExtendWith(MinecraftBootstrapExtension.class)
public class InventorySwapperTest {

    private ItemStack[] slots;

    @BeforeEach
    public void setUpInventory() {
        slots = new ItemStack[Inventory.INVENTORY_SIZE];
        for (int i = 0; i < slots.length; i++) {
            slots[i] = ItemStack.EMPTY;
        }
    }

    private int find(int selectedSlot, ItemStack target) {
        return InventorySwapper.findMatchingSlot(i -> slots[i], selectedSlot, target);
    }

    private static ItemStack named(net.minecraft.world.item.Item item, String name) {
        ItemStack stack = new ItemStack(item);
        stack.set(DataComponents.CUSTOM_NAME, Component.literal(name));
        return stack;
    }

    // --- Stage 1: exact match (item + components) ---

    @Test
    public void testFindsExactMatch() {
        slots[12] = new ItemStack(Items.GRANITE);
        assertEquals(12, find(0, new ItemStack(Items.GRANITE)));
    }

    @Test
    public void testReturnsMinusOneWhenNotFound() {
        slots[12] = new ItemStack(Items.GRANITE);
        assertEquals(-1, find(0, new ItemStack(Items.DIORITE)));
    }

    @Test
    public void testReturnsMinusOneForEmptyInventory() {
        assertEquals(-1, find(0, new ItemStack(Items.STONE)));
    }

    @Test
    public void testSkipsSelectedSlot() {
        // The only match sits in the selected slot itself - swapping it with
        // itself is pointless, so it must not be offered
        slots[3] = new ItemStack(Items.GRANITE);
        assertEquals(-1, find(3, new ItemStack(Items.GRANITE)));
    }

    @Test
    public void testFindsMatchInOtherSlotEvenIfSelectedSlotMatches() {
        slots[3] = new ItemStack(Items.GRANITE);
        slots[20] = new ItemStack(Items.GRANITE);
        assertEquals(20, find(3, new ItemStack(Items.GRANITE)));
    }

    @Test
    public void testExactComponentMatchWins() {
        // A plain granite sits earlier, but the target has components:
        // only the exact component match may be returned
        slots[5] = new ItemStack(Items.GRANITE);
        slots[10] = named(Items.GRANITE, "Special");

        assertEquals(10, find(0, named(Items.GRANITE, "Special")));
    }

    @Test
    public void testCountDifferenceStillMatches() {
        slots[7] = new ItemStack(Items.GRANITE, 42);
        assertEquals(7, find(0, new ItemStack(Items.GRANITE)));
    }

    // --- Stage 2: base-item fallback ---

    @Test
    public void testFallbackToBaseItemWhenTargetHasNoComponents() {
        // Target is plain granite; inventory only has a renamed granite.
        // The plain target may fall back to the base item.
        slots[9] = named(Items.GRANITE, "Renamed");
        assertEquals(9, find(0, new ItemStack(Items.GRANITE)));
    }

    @Test
    public void testNoFallbackWhenTargetHasComponents() {
        // Target has components; a plain granite must NOT be treated as a match
        slots[9] = new ItemStack(Items.GRANITE);
        assertEquals(-1, find(0, named(Items.GRANITE, "Special")));
    }

    @Test
    public void testExactMatchPreferredOverFallback() {
        // Renamed granite sits in an earlier slot than the exact plain match;
        // stage 1 (exact) must win even though stage 2 would find slot 4 first
        slots[4] = named(Items.GRANITE, "Renamed");
        slots[30] = new ItemStack(Items.GRANITE);
        assertEquals(30, find(0, new ItemStack(Items.GRANITE)));
    }

    @Test
    public void testFallbackSkipsSelectedSlot() {
        slots[2] = named(Items.GRANITE, "Renamed");
        assertEquals(-1, find(2, new ItemStack(Items.GRANITE)));
    }

    // --- Inventory index -> InventoryMenu slot index conversion ---

    @Test
    public void testHotbarSlotsMapToMenuHotbarRow() {
        assertEquals(InventoryMenu.USE_ROW_SLOT_START, InventorySwapper.inventorySlotToMenuSlot(0));
        assertEquals(InventoryMenu.USE_ROW_SLOT_START + 8, InventorySwapper.inventorySlotToMenuSlot(8));
    }

    @Test
    public void testMainInventorySlotsMapToSameIndex() {
        assertEquals(9, InventorySwapper.inventorySlotToMenuSlot(9));
        assertEquals(35, InventorySwapper.inventorySlotToMenuSlot(35));
    }
}
