package com.github.aruma256.pocketpalette.client.palette;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.github.aruma256.pocketpalette.MinecraftBootstrapExtension;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

@ExtendWith(MinecraftBootstrapExtension.class)
public class ItemPaletteTest {

    @BeforeEach
    public void resetPalette() {
        ItemPalette.clear();
    }

    // --- Tests for new API ---

    @Test
    public void testCanCycle_returnsFalseForEmptyPalette() {
        assertFalse(ItemPalette.canCycle(new ItemStack(Items.STONE), 0));
    }

    @Test
    public void testCanCycle_returnsFalseForNullInput() {
        assertFalse(ItemPalette.canCycle(null, 0));
    }

    @Test
    public void testCanCycle_returnsFalseForEmptyStack() {
        assertFalse(ItemPalette.canCycle(ItemStack.EMPTY, 0));
    }

    @Test
    public void testCanCycle_returnsFalseForAir() {
        assertFalse(ItemPalette.canCycle(new ItemStack(Items.AIR), 0));
    }

    @Test
    public void testCanCycle_returnsFalseForInvalidGroupIndex() {
        loadSimpleCycle(0);
        assertFalse(ItemPalette.canCycle(new ItemStack(Items.STONE), 1));
        assertFalse(ItemPalette.canCycle(new ItemStack(Items.STONE), -1));
    }

    @Test
    public void testCanCycle_returnsTrueForRegisteredItem() {
        loadSimpleCycle(0);

        assertTrue(ItemPalette.canCycle(new ItemStack(Items.STONE), 0));
        assertTrue(ItemPalette.canCycle(new ItemStack(Items.GRANITE), 0));
    }

    @Test
    public void testCanCycle_returnsFalseForUnregisteredItem() {
        loadSimpleCycle(0);

        assertFalse(ItemPalette.canCycle(new ItemStack(Items.DIORITE), 0));
    }

    @Test
    public void testGetAllCycleItems_returnsNullForNullInput() {
        assertNull(ItemPalette.getAllCycleItems(null, 0));
    }

    @Test
    public void testGetAllCycleItems_returnsNullForEmptyStack() {
        assertNull(ItemPalette.getAllCycleItems(ItemStack.EMPTY, 0));
    }

    @Test
    public void testGetAllCycleItems_returnsNullForUnregisteredItem() {
        assertNull(ItemPalette.getAllCycleItems(new ItemStack(Items.STONE), 0));
    }

    @Test
    public void testGetAllCycleItems_returnsCycleList() {
        List<ItemState> cycle = Arrays.asList(
                new ItemState(new ItemStack(Items.STONE)),
                new ItemState(new ItemStack(Items.GRANITE)),
                new ItemState(new ItemStack(Items.DIORITE))
        );
        loadCycle(0, cycle);

        List<ItemStack> result = ItemPalette.getAllCycleItems(new ItemStack(Items.STONE), 0);

        assertNotNull(result);
        assertEquals(3, result.size());
        assertEquals(Items.STONE, result.get(0).getItem());
        assertEquals(Items.GRANITE, result.get(1).getItem());
        assertEquals(Items.DIORITE, result.get(2).getItem());
    }

    @Test
    public void testGetAllCycleItems_startsFromGivenItem() {
        List<ItemState> cycle = Arrays.asList(
                new ItemState(new ItemStack(Items.STONE)),
                new ItemState(new ItemStack(Items.GRANITE)),
                new ItemState(new ItemStack(Items.DIORITE))
        );
        loadCycle(0, cycle);

        List<ItemStack> result = ItemPalette.getAllCycleItems(new ItemStack(Items.GRANITE), 0);

        assertNotNull(result);
        assertEquals(3, result.size());
        assertEquals(Items.GRANITE, result.get(0).getItem());
        assertEquals(Items.DIORITE, result.get(1).getItem());
        assertEquals(Items.STONE, result.get(2).getItem());
    }

    @Test
    public void testGroups_areIndependent() {
        List<ItemState> cycle0 = Arrays.asList(
                new ItemState(new ItemStack(Items.STONE)),
                new ItemState(new ItemStack(Items.GRANITE))
        );
        List<ItemState> cycle1 = Arrays.asList(
                new ItemState(new ItemStack(Items.OAK_PLANKS)),
                new ItemState(new ItemStack(Items.SPRUCE_PLANKS))
        );

        List<List<List<ItemState>>> groups = new ArrayList<>();
        groups.add(List.of(cycle0)); // Group 0
        groups.add(List.of(cycle1)); // Group 1
        ItemPalette.loadGroups(groups);

        assertTrue(ItemPalette.canCycle(new ItemStack(Items.STONE), 0));
        assertFalse(ItemPalette.canCycle(new ItemStack(Items.STONE), 1));

        assertTrue(ItemPalette.canCycle(new ItemStack(Items.OAK_PLANKS), 1));
        assertFalse(ItemPalette.canCycle(new ItemStack(Items.OAK_PLANKS), 0));
    }

    @Test
    public void testGetGroupCount() {
        assertEquals(0, ItemPalette.getGroupCount());

        List<List<List<ItemState>>> groups = new ArrayList<>();
        groups.add(new ArrayList<>()); // Group 0
        groups.add(new ArrayList<>()); // Group 1
        groups.add(new ArrayList<>()); // Group 2
        ItemPalette.loadGroups(groups);

        assertEquals(3, ItemPalette.getGroupCount());
    }

    @Test
    public void testClear_removesAllGroups() {
        loadSimpleCycle(0);
        assertTrue(ItemPalette.canCycle(new ItemStack(Items.STONE), 0));

        ItemPalette.clear();

        assertEquals(0, ItemPalette.getGroupCount());
        assertFalse(ItemPalette.canCycle(new ItemStack(Items.STONE), 0));
    }

    // --- Tests for containsExactItem (no fallback) ---

    @Test
    public void testContainsExactItem_returnsTrueForExactMatch() {
        List<ItemState> cycle = Arrays.asList(
                new ItemState(new ItemStack(Items.STONE)),
                new ItemState(new ItemStack(Items.GRANITE))
        );
        loadCycle(0, cycle);

        assertTrue(ItemPalette.containsExactItem(new ItemStack(Items.STONE), 0));
        assertTrue(ItemPalette.containsExactItem(new ItemStack(Items.GRANITE), 0));
    }

    @Test
    public void testContainsExactItem_returnsFalseForItemWithDifferentComponents() {
        // Register plain stone
        List<ItemState> cycle = Arrays.asList(
                new ItemState(new ItemStack(Items.STONE)),
                new ItemState(new ItemStack(Items.GRANITE))
        );
        loadCycle(0, cycle);

        // Named stone should NOT match (no fallback in containsExactItem)
        ItemStack namedStone = new ItemStack(Items.STONE);
        namedStone.set(DataComponents.CUSTOM_NAME, Component.literal("Special Stone"));

        assertFalse(ItemPalette.containsExactItem(namedStone, 0));
        // But canCycle should still return true due to fallback
        assertTrue(ItemPalette.canCycle(namedStone, 0));
    }

    @Test
    public void testContainsExactItem_returnsFalseForUnregisteredItem() {
        loadSimpleCycle(0);
        assertFalse(ItemPalette.containsExactItem(new ItemStack(Items.DIORITE), 0));
    }

    @Test
    public void testContainsExactItem_allowsSameItemWithDifferentComponentsInSeparateCycles() {
        // This tests the use case: plain bow registered, want to add enchanted bow
        ItemStack namedStone = new ItemStack(Items.STONE);
        namedStone.set(DataComponents.CUSTOM_NAME, Component.literal("Named Stone"));

        // Plain stone is registered
        List<ItemState> plainCycle = Arrays.asList(
                new ItemState(new ItemStack(Items.STONE)),
                new ItemState(new ItemStack(Items.GRANITE))
        );
        loadCycle(0, plainCycle);

        // containsExactItem should return false for named stone (allowing it to be added)
        assertFalse(ItemPalette.containsExactItem(namedStone, 0));
        // canCycle returns true due to fallback (for runtime cycling)
        assertTrue(ItemPalette.canCycle(namedStone, 0));
    }

    // --- Tests for two-stage fallback ---

    @Test
    public void testFallback_plainItemMatchesWhenNoComponents() {
        // Register plain stone in cycle
        List<ItemState> cycle = Arrays.asList(
                new ItemState(new ItemStack(Items.STONE)),
                new ItemState(new ItemStack(Items.GRANITE))
        );
        loadCycle(0, cycle);

        // Query with named stone should fallback to plain stone
        ItemStack namedStone = new ItemStack(Items.STONE);
        namedStone.set(DataComponents.CUSTOM_NAME, Component.literal("Special Stone"));

        assertTrue(ItemPalette.canCycle(namedStone, 0));
        List<ItemStack> result = ItemPalette.getAllCycleItems(namedStone, 0);
        assertNotNull(result);
        assertEquals(2, result.size());
    }

    @Test
    public void testExactMatch_prefersComponentMatch() {
        // Register BOTH plain stone and named stone as different cycles
        ItemStack namedStone = new ItemStack(Items.STONE);
        namedStone.set(DataComponents.CUSTOM_NAME, Component.literal("Special Stone"));

        List<ItemState> plainCycle = Arrays.asList(
                new ItemState(new ItemStack(Items.STONE)),
                new ItemState(new ItemStack(Items.GRANITE))
        );
        List<ItemState> namedCycle = Arrays.asList(
                new ItemState(namedStone),
                new ItemState(new ItemStack(Items.DIORITE))
        );

        List<List<List<ItemState>>> groups = new ArrayList<>();
        groups.add(Arrays.asList(plainCycle, namedCycle));
        ItemPalette.loadGroups(groups);

        // Query with named stone should return the named cycle
        List<ItemStack> result = ItemPalette.getAllCycleItems(namedStone.copy(), 0);
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(Items.DIORITE, result.get(1).getItem());

        // Query with plain stone should return the plain cycle
        result = ItemPalette.getAllCycleItems(new ItemStack(Items.STONE), 0);
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(Items.GRANITE, result.get(1).getItem());
    }

    // --- Helper methods ---

    private void loadSimpleCycle(int groupIndex) {
        List<ItemState> cycle = Arrays.asList(
                new ItemState(new ItemStack(Items.STONE)),
                new ItemState(new ItemStack(Items.GRANITE))
        );
        loadCycle(groupIndex, cycle);
    }

    private void loadCycle(int groupIndex, List<ItemState> cycle) {
        List<List<List<ItemState>>> groups = new ArrayList<>();
        while (groups.size() <= groupIndex) {
            groups.add(new ArrayList<>());
        }
        groups.get(groupIndex).add(cycle);
        ItemPalette.loadGroups(groups);
    }
}
