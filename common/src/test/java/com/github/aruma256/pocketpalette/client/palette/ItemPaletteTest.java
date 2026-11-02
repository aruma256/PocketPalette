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

    // --- Cycle lookup ---

    @Test
    public void testGetCycleEntries_returnsNullForEmptyPalette() {
        assertNull(ItemPalette.getCycleEntries(new ItemStack(Items.STONE), 0));
    }

    @Test
    public void testGetCycleEntries_returnsNullForNullInput() {
        loadSimpleCycle(0);
        assertNull(ItemPalette.getCycleEntries(null, 0));
    }

    @Test
    public void testGetCycleEntries_returnsNullForEmptyStack() {
        loadSimpleCycle(0);
        assertNull(ItemPalette.getCycleEntries(ItemStack.EMPTY, 0));
    }

    @Test
    public void testGetCycleEntries_returnsNullForAir() {
        loadSimpleCycle(0);
        assertNull(ItemPalette.getCycleEntries(new ItemStack(Items.AIR), 0));
    }

    @Test
    public void testGetCycleEntries_returnsNullForInvalidGroupIndex() {
        loadSimpleCycle(0);
        assertNull(ItemPalette.getCycleEntries(new ItemStack(Items.STONE), 1));
        assertNull(ItemPalette.getCycleEntries(new ItemStack(Items.STONE), -1));
    }

    @Test
    public void testGetCycleEntries_returnsCycleForRegisteredItem() {
        loadSimpleCycle(0);

        assertNotNull(ItemPalette.getCycleEntries(new ItemStack(Items.STONE), 0));
        assertNotNull(ItemPalette.getCycleEntries(new ItemStack(Items.GRANITE), 0));
    }

    @Test
    public void testGetCycleEntries_returnsNullForUnregisteredItem() {
        loadSimpleCycle(0);

        assertNull(ItemPalette.getCycleEntries(new ItemStack(Items.DIORITE), 0));
    }

    @Test
    public void testGetCycleEntries_returnsCycleList() {
        List<ItemState> cycle = Arrays.asList(
                new ItemState(new ItemStack(Items.STONE)),
                new ItemState(new ItemStack(Items.GRANITE)),
                new ItemState(new ItemStack(Items.DIORITE))
        );
        loadCycle(0, cycle);

        List<ItemState> result = ItemPalette.getCycleEntries(new ItemStack(Items.STONE), 0);

        assertEquals(cycle, result);
    }

    @Test
    public void testGetCycleEntries_startsFromGivenItem() {
        List<ItemState> cycle = Arrays.asList(
                new ItemState(new ItemStack(Items.STONE)),
                new ItemState(new ItemStack(Items.GRANITE)),
                new ItemState(new ItemStack(Items.DIORITE))
        );
        loadCycle(0, cycle);

        List<ItemState> result = ItemPalette.getCycleEntries(new ItemStack(Items.GRANITE), 0);

        assertEquals(List.of(cycle.get(1), cycle.get(2), cycle.get(0)), result);
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

        assertNotNull(ItemPalette.getCycleEntries(new ItemStack(Items.STONE), 0));
        assertNull(ItemPalette.getCycleEntries(new ItemStack(Items.STONE), 1));

        assertNotNull(ItemPalette.getCycleEntries(new ItemStack(Items.OAK_PLANKS), 1));
        assertNull(ItemPalette.getCycleEntries(new ItemStack(Items.OAK_PLANKS), 0));
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
        assertNotNull(ItemPalette.getCycleEntries(new ItemStack(Items.STONE), 0));

        ItemPalette.clear();

        assertEquals(0, ItemPalette.getGroupCount());
        assertNull(ItemPalette.getCycleEntries(new ItemStack(Items.STONE), 0));
    }

    // --- Tests for two-stage fallback ---

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
        List<ItemState> result = ItemPalette.getCycleEntries(namedStone.copy(), 0);
        assertEquals(namedCycle, result);

        // Query with plain stone should return the plain cycle
        result = ItemPalette.getCycleEntries(new ItemStack(Items.STONE), 0);
        assertEquals(plainCycle, result);
    }

    @Test
    public void testGetCycleEntries_returnsNullForDifferentComponentsWithoutBaseCycle() {
        ItemStack namedStone = new ItemStack(Items.STONE);
        namedStone.set(DataComponents.CUSTOM_NAME, Component.literal("Special Stone"));
        loadCycle(0, List.of(new ItemState(namedStone), new ItemState(new ItemStack(Items.GRANITE))));

        ItemStack otherStone = new ItemStack(Items.STONE);
        otherStone.set(DataComponents.CUSTOM_NAME, Component.literal("Other Stone"));

        assertNull(ItemPalette.getCycleEntries(otherStone, 0));
        assertNull(ItemPalette.getCycleEntries(new ItemStack(Items.STONE), 0));
    }

    @Test
    public void testGetCycleEntries_preservesComponentlessStartEntryOnFallback() {
        // Runtime display keeps the actual stack, but inventory matching must
        // retain the componentless palette entry that accepted it. Otherwise
        // a damaged or enchanted tool becomes a component-exact query and
        // cannot find a pristine copy of the same tool.
        loadSimpleCycle(0);

        ItemStack namedStone = new ItemStack(Items.STONE);
        namedStone.set(DataComponents.CUSTOM_NAME, Component.literal("Special Stone"));

        List<ItemState> entries = ItemPalette.getCycleEntries(namedStone, 0);

        assertNotNull(entries);
        assertEquals(2, entries.size());
        assertEquals(new ItemState(new ItemStack(Items.STONE)), entries.get(0));
        assertFalse(entries.get(0).hasComponents());
        assertEquals(new ItemState(new ItemStack(Items.GRANITE)), entries.get(1));
    }

    @Test
    public void testLoadGroups_skipsCyclesWithLessThan2Items() {
        List<ItemState> tooShort = List.of(new ItemState(new ItemStack(Items.STONE)));
        loadCycle(0, tooShort);

        assertNull(ItemPalette.getCycleEntries(new ItemStack(Items.STONE), 0));
    }

    @Test
    public void testLoadGroups_firstCycleWinsOnDuplicateItem() {
        // STONE appears in both cycles; the first registration must win
        List<ItemState> cycleA = Arrays.asList(
                new ItemState(new ItemStack(Items.STONE)),
                new ItemState(new ItemStack(Items.GRANITE))
        );
        List<ItemState> cycleB = Arrays.asList(
                new ItemState(new ItemStack(Items.STONE)),
                new ItemState(new ItemStack(Items.DIORITE))
        );
        List<List<List<ItemState>>> groups = new ArrayList<>();
        groups.add(Arrays.asList(cycleA, cycleB));
        ItemPalette.loadGroups(groups);

        List<ItemState> result = ItemPalette.getCycleEntries(new ItemStack(Items.STONE), 0);

        assertNotNull(result);
        assertEquals(new ItemState(new ItemStack(Items.GRANITE)), result.get(1));
        // DIORITE (only in cycleB) must still resolve to cycleB
        List<ItemState> dioriteResult = ItemPalette.getCycleEntries(new ItemStack(Items.DIORITE), 0);
        assertNotNull(dioriteResult);
        assertEquals(new ItemState(new ItemStack(Items.STONE)), dioriteResult.get(1));
    }

    @Test
    public void testGetGroupData_returnsLoadedData() {
        List<ItemState> cycle = Arrays.asList(
                new ItemState(new ItemStack(Items.STONE)),
                new ItemState(new ItemStack(Items.GRANITE))
        );
        loadCycle(0, cycle);

        List<List<List<ItemState>>> data = ItemPalette.getGroupData();

        assertEquals(1, data.size());
        assertEquals(1, data.get(0).size());
        assertEquals(cycle, data.get(0).get(0));
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
