package com.github.aruma256.pocketpalette.client.keybinding;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;

import com.github.aruma256.pocketpalette.MinecraftBootstrapExtension;
import com.github.aruma256.pocketpalette.client.palette.ItemPalette;
import com.github.aruma256.pocketpalette.client.palette.ItemState;

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

    private List<Integer> findAll(int selectedSlot, ItemStack target) {
        return InventorySwapper.findMatchingSlots(i -> slots[i], selectedSlot, new ItemState(target));
    }

    private int find(int selectedSlot, ItemStack target) {
        List<Integer> found = findAll(selectedSlot, target);
        return found.isEmpty() ? -1 : found.get(0);
    }

    private static ItemStack named(net.minecraft.world.item.Item item, String name) {
        ItemStack stack = new ItemStack(item);
        stack.set(DataComponents.CUSTOM_NAME, Component.literal(name));
        return stack;
    }

    // --- Basic matching behavior ---

    @Test
    public void testFindsExactMatch() {
        slots[12] = new ItemStack(Items.GRANITE);
        assertEquals(12, find(0, new ItemStack(Items.GRANITE)));
    }

    @Test
    public void testReturnsEmptyWhenNotFound() {
        slots[12] = new ItemStack(Items.GRANITE);
        assertEquals(List.of(), findAll(0, new ItemStack(Items.DIORITE)));
    }

    @Test
    public void testReturnsEmptyForEmptyInventory() {
        assertEquals(List.of(), findAll(0, new ItemStack(Items.STONE)));
    }

    @Test
    public void testSkipsSelectedSlot() {
        // The only match sits in the selected slot itself - swapping it with
        // itself is pointless, so it must not be offered
        slots[3] = new ItemStack(Items.GRANITE);
        assertEquals(List.of(), findAll(3, new ItemStack(Items.GRANITE)));
    }

    @Test
    public void testFindsMatchInOtherSlotEvenIfSelectedSlotMatches() {
        slots[3] = new ItemStack(Items.GRANITE);
        slots[20] = new ItemStack(Items.GRANITE);
        assertEquals(List.of(20), findAll(3, new ItemStack(Items.GRANITE)));
    }

    @Test
    public void testExactComponentMatchWins() {
        // A plain granite sits earlier, but the target has components:
        // only the exact component match may be returned
        slots[5] = new ItemStack(Items.GRANITE);
        slots[10] = named(Items.GRANITE, "Special");

        assertEquals(List.of(10), findAll(0, named(Items.GRANITE, "Special")));
    }

    @Test
    public void testCountDifferenceStillMatches() {
        slots[7] = new ItemStack(Items.GRANITE, 42);
        assertEquals(7, find(0, new ItemStack(Items.GRANITE)));
    }

    // --- Multiple matching stacks ---

    @Test
    public void testMultipleStacksOrderedByCountAscending() {
        slots[10] = new ItemStack(Items.TORCH, 30);
        slots[20] = new ItemStack(Items.TORCH, 64);
        slots[25] = new ItemStack(Items.TORCH, 5);
        assertEquals(List.of(25, 10, 20), findAll(0, new ItemStack(Items.TORCH)));
    }

    @Test
    public void testEqualCountsKeepSlotOrder() {
        slots[15] = new ItemStack(Items.TORCH, 64);
        slots[5] = new ItemStack(Items.TORCH, 64);
        assertEquals(List.of(5, 15), findAll(0, new ItemStack(Items.TORCH)));
    }

    @Test
    public void testEqualCountsOrderedByRemainingDurabilityAscendingThenSlot() {
        slots[3] = new ItemStack(Items.DIAMOND_PICKAXE);
        slots[10] = new ItemStack(Items.DIAMOND_PICKAXE);
        slots[10].setDamageValue(100);
        slots[20] = new ItemStack(Items.DIAMOND_PICKAXE);
        slots[20].setDamageValue(500);
        slots[25] = slots[20].copy();
        // The held tool is excluded even when it would sort between candidates.
        slots[0] = new ItemStack(Items.DIAMOND_PICKAXE);
        slots[0].setDamageValue(200);

        assertEquals(List.of(20, 25, 10, 3), findAll(0, new ItemStack(Items.DIAMOND_PICKAXE)));
    }

    @Test
    public void testComponentlessPaletteEntryIncludesAllBaseItemVariants() {
        // A componentless palette entry acts as a base-item wildcard. A
        // pristine tool must not hide damaged variants merely because it is
        // also an exact match for the entry.
        ItemStack damagedPickaxe = new ItemStack(Items.DIAMOND_PICKAXE);
        damagedPickaxe.setDamageValue(100);
        slots[4] = damagedPickaxe;
        slots[30] = new ItemStack(Items.DIAMOND_PICKAXE);

        assertEquals(List.of(4, 30), findAll(0, new ItemStack(Items.DIAMOND_PICKAXE)));
    }

    @Test
    public void testComponentlessToolCycleMatchesPristineAndDamagedStacksBothDirections() {
        ItemState pickaxeEntry = new ItemState(new ItemStack(Items.DIAMOND_PICKAXE));
        ItemPalette.loadGroups(List.of(List.of(List.of(
                pickaxeEntry,
                new ItemState(new ItemStack(Items.DIAMOND_SHOVEL))))));

        ItemStack damagedPickaxe = new ItemStack(Items.DIAMOND_PICKAXE);
        damagedPickaxe.setDamageValue(100);
        slots[0] = damagedPickaxe;
        slots[9] = new ItemStack(Items.DIAMOND_PICKAXE);

        ItemState entryFromDamaged = ItemPalette.getCycleEntries(damagedPickaxe, 0).getFirst();
        ItemState entryFromPristine = ItemPalette.getCycleEntries(slots[9], 0).getFirst();

        assertEquals(pickaxeEntry, entryFromDamaged);
        assertEquals(List.of(9),
                InventorySwapper.findMatchingSlots(i -> slots[i], 0, entryFromDamaged));
        assertEquals(List.of(0),
                InventorySwapper.findMatchingSlots(i -> slots[i], 9, entryFromPristine));
    }

    // --- Componentless palette entries ---

    @Test
    public void testFallbackToBaseItemWhenTargetHasNoComponents() {
        // Target is plain granite; inventory only has a renamed granite.
        // The plain target may fall back to the base item.
        slots[9] = named(Items.GRANITE, "Renamed");
        assertEquals(List.of(9), findAll(0, new ItemStack(Items.GRANITE)));
    }

    @Test
    public void testNoFallbackWhenTargetHasComponents() {
        // Target has components; a plain granite must NOT be treated as a match
        slots[9] = new ItemStack(Items.GRANITE);
        assertEquals(List.of(), findAll(0, named(Items.GRANITE, "Special")));
    }

    @Test
    public void testFallbackReturnsAllBaseItemMatches() {
        ItemStack renamedA = named(Items.GRANITE, "A");
        renamedA.setCount(3);
        ItemStack renamedB = named(Items.GRANITE, "B");
        renamedB.setCount(40);
        slots[9] = renamedA;
        slots[11] = renamedB;
        assertEquals(List.of(9, 11), findAll(0, new ItemStack(Items.GRANITE)));
    }

    @Test
    public void testFallbackSkipsSelectedSlot() {
        slots[2] = named(Items.GRANITE, "Renamed");
        assertEquals(List.of(), findAll(2, new ItemStack(Items.GRANITE)));
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
