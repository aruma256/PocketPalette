package com.github.aruma256.pocketpalette.client.palette;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

import com.github.aruma256.pocketpalette.MinecraftBootstrapExtension;
import com.github.aruma256.pocketpalette.client.palette.PaletteEditModel.AddResult;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

@ExtendWith(MinecraftBootstrapExtension.class)
public class PaletteEditModelTest {

    private static ItemState state(net.minecraft.world.item.Item item) {
        return new ItemState(new ItemStack(item));
    }

    private static List<ItemState> cycle(net.minecraft.world.item.Item... items) {
        List<ItemState> cycle = new ArrayList<>();
        for (net.minecraft.world.item.Item item : items) {
            cycle.add(state(item));
        }
        return cycle;
    }

    private static List<List<List<ItemState>>> groupDataWithStoneCycle() {
        List<List<List<ItemState>>> groupData = new ArrayList<>();
        List<List<ItemState>> group0 = new ArrayList<>();
        group0.add(cycle(Items.STONE, Items.GRANITE));
        group0.add(cycle(Items.OAK_PLANKS, Items.SPRUCE_PLANKS));
        groupData.add(group0);
        groupData.add(new ArrayList<>());
        return groupData;
    }

    // --- Construction / cycle resolution ---

    @Test
    public void testOpensExistingCycleContainingSeedItem() {
        List<List<List<ItemState>>> groupData = groupDataWithStoneCycle();
        PaletteEditModel model = new PaletteEditModel(groupData, new ItemStack(Items.GRANITE));

        assertFalse(model.isNewCycle(0));
        assertEquals(cycle(Items.STONE, Items.GRANITE), model.getCycle(0));
    }

    @Test
    public void testStartsNewCycleSeededWithBaseItem() {
        List<List<List<ItemState>>> groupData = groupDataWithStoneCycle();
        PaletteEditModel model = new PaletteEditModel(groupData, new ItemStack(Items.DIORITE));

        assertTrue(model.isNewCycle(0));
        assertEquals(cycle(Items.DIORITE), model.getCycle(0));
    }

    @Test
    public void testSeedWithComponentsFallsBackToBaseItemCycle() {
        List<List<List<ItemState>>> groupData = groupDataWithStoneCycle();
        ItemStack named = new ItemStack(Items.STONE);
        named.set(DataComponents.CUSTOM_NAME, Component.literal("renamed"));

        PaletteEditModel model = new PaletteEditModel(groupData, named);

        assertFalse(model.isNewCycle(0));
        assertEquals(cycle(Items.STONE, Items.GRANITE), model.getCycle(0));
    }

    @Test
    public void testGroupsAreResolvedIndependently() {
        List<List<List<ItemState>>> groupData = groupDataWithStoneCycle();
        groupData.get(1).add(cycle(Items.STONE, Items.DIORITE));

        PaletteEditModel model = new PaletteEditModel(groupData, new ItemStack(Items.STONE));

        assertEquals(cycle(Items.STONE, Items.GRANITE), model.getCycle(0));
        assertEquals(cycle(Items.STONE, Items.DIORITE), model.getCycle(1));
    }

    @Test
    public void testGuaranteesAtLeastTwoGroups() {
        List<List<List<ItemState>>> groupData = new ArrayList<>();
        PaletteEditModel model = new PaletteEditModel(groupData, new ItemStack(Items.STONE));

        assertEquals(2, model.getGroupCount());
        assertTrue(model.isNewCycle(0));
        assertTrue(model.isNewCycle(1));
    }

    // --- add ---

    @Test
    public void testAddAppendsBaseItem() {
        List<List<List<ItemState>>> groupData = groupDataWithStoneCycle();
        PaletteEditModel model = new PaletteEditModel(groupData, new ItemStack(Items.STONE));

        assertEquals(AddResult.ADDED, model.add(0, new ItemStack(Items.DIORITE)));
        assertEquals(cycle(Items.STONE, Items.GRANITE, Items.DIORITE), model.getCycle(0));
    }

    @Test
    public void testAddStripsComponents() {
        List<List<List<ItemState>>> groupData = groupDataWithStoneCycle();
        PaletteEditModel model = new PaletteEditModel(groupData, new ItemStack(Items.STONE));

        ItemStack named = new ItemStack(Items.DIORITE);
        named.set(DataComponents.CUSTOM_NAME, Component.literal("renamed"));

        assertEquals(AddResult.ADDED, model.add(0, named));
        assertEquals(state(Items.DIORITE), model.getCycle(0).get(2));
    }

    @Test
    public void testAddRejectsDuplicateInCycle() {
        List<List<List<ItemState>>> groupData = groupDataWithStoneCycle();
        PaletteEditModel model = new PaletteEditModel(groupData, new ItemStack(Items.STONE));

        assertEquals(AddResult.DUPLICATE_IN_CYCLE, model.add(0, new ItemStack(Items.GRANITE)));
    }

    @Test
    public void testAddRejectsItemUsedByAnotherCycleInSameGroup() {
        List<List<List<ItemState>>> groupData = groupDataWithStoneCycle();
        PaletteEditModel model = new PaletteEditModel(groupData, new ItemStack(Items.STONE));

        assertEquals(AddResult.DUPLICATE_IN_GROUP, model.add(0, new ItemStack(Items.OAK_PLANKS)));
    }

    @Test
    public void testAddAllowsItemUsedInOtherGroup() {
        List<List<List<ItemState>>> groupData = groupDataWithStoneCycle();
        PaletteEditModel model = new PaletteEditModel(groupData, new ItemStack(Items.DIORITE));

        // OAK_PLANKS is used in group 0 but not in group 1
        assertEquals(AddResult.ADDED, model.add(1, new ItemStack(Items.OAK_PLANKS)));
    }

    @Test
    public void testAddRejectsInvalidStacks() {
        List<List<List<ItemState>>> groupData = groupDataWithStoneCycle();
        PaletteEditModel model = new PaletteEditModel(groupData, new ItemStack(Items.STONE));

        assertEquals(AddResult.INVALID, model.add(0, ItemStack.EMPTY));
        assertEquals(AddResult.INVALID, model.add(0, null));
    }

    // --- reorder / remove ---

    @Test
    public void testMoveUpAndDown() {
        List<List<List<ItemState>>> groupData = groupDataWithStoneCycle();
        PaletteEditModel model = new PaletteEditModel(groupData, new ItemStack(Items.STONE));
        model.add(0, new ItemStack(Items.DIORITE));

        assertTrue(model.moveUp(0, 2));
        assertEquals(cycle(Items.STONE, Items.DIORITE, Items.GRANITE), model.getCycle(0));

        assertTrue(model.moveDown(0, 1));
        assertEquals(cycle(Items.STONE, Items.GRANITE, Items.DIORITE), model.getCycle(0));
    }

    @Test
    public void testMoveRejectsOutOfRange() {
        List<List<List<ItemState>>> groupData = groupDataWithStoneCycle();
        PaletteEditModel model = new PaletteEditModel(groupData, new ItemStack(Items.STONE));

        assertFalse(model.moveUp(0, 0));
        assertFalse(model.moveDown(0, 1));
        assertFalse(model.moveUp(0, 5));
        assertFalse(model.remove(0, 5));
    }

    @Test
    public void testRemove() {
        List<List<List<ItemState>>> groupData = groupDataWithStoneCycle();
        PaletteEditModel model = new PaletteEditModel(groupData, new ItemStack(Items.STONE));

        assertTrue(model.remove(0, 0));
        assertEquals(cycle(Items.GRANITE), model.getCycle(0));
        assertTrue(model.willDiscardCycle(0));
    }

    // --- commit ---

    @Test
    public void testCommitDoesNothingWhenUntouched() {
        List<List<List<ItemState>>> groupData = groupDataWithStoneCycle();
        PaletteEditModel model = new PaletteEditModel(groupData, new ItemStack(Items.STONE));

        assertFalse(model.commit());
        assertEquals(2, groupData.get(0).size());
    }

    @Test
    public void testCommitReplacesEditedCycleInPlace() {
        List<List<List<ItemState>>> groupData = groupDataWithStoneCycle();
        PaletteEditModel model = new PaletteEditModel(groupData, new ItemStack(Items.STONE));
        model.add(0, new ItemStack(Items.DIORITE));

        assertTrue(model.commit());
        assertEquals(cycle(Items.STONE, Items.GRANITE, Items.DIORITE), groupData.get(0).get(0));
        assertEquals(cycle(Items.OAK_PLANKS, Items.SPRUCE_PLANKS), groupData.get(0).get(1));
    }

    @Test
    public void testCommitAppendsNewCycle() {
        List<List<List<ItemState>>> groupData = groupDataWithStoneCycle();
        PaletteEditModel model = new PaletteEditModel(groupData, new ItemStack(Items.DIORITE));
        model.add(0, new ItemStack(Items.ANDESITE));

        assertTrue(model.commit());
        assertEquals(3, groupData.get(0).size());
        assertEquals(cycle(Items.DIORITE, Items.ANDESITE), groupData.get(0).get(2));
    }

    @Test
    public void testCommitDropsNewCycleWithFewerThanTwoItems() {
        List<List<List<ItemState>>> groupData = groupDataWithStoneCycle();
        PaletteEditModel model = new PaletteEditModel(groupData, new ItemStack(Items.DIORITE));

        assertFalse(model.commit());
        assertEquals(2, groupData.get(0).size());
    }

    @Test
    public void testCommitRemovesExistingCycleReducedBelowTwoItems() {
        List<List<List<ItemState>>> groupData = groupDataWithStoneCycle();
        PaletteEditModel model = new PaletteEditModel(groupData, new ItemStack(Items.STONE));
        model.remove(0, 1);
        model.remove(0, 0);

        assertTrue(model.commit());
        assertEquals(1, groupData.get(0).size());
        assertEquals(cycle(Items.OAK_PLANKS, Items.SPRUCE_PLANKS), groupData.get(0).get(0));
    }

    @Test
    public void testCommitPadsMissingGroups() {
        List<List<List<ItemState>>> groupData = new ArrayList<>();
        PaletteEditModel model = new PaletteEditModel(groupData, new ItemStack(Items.STONE));
        model.add(1, new ItemStack(Items.GRANITE));

        assertTrue(model.commit());
        assertEquals(2, groupData.size());
        assertTrue(groupData.get(0).isEmpty());
        assertEquals(cycle(Items.STONE, Items.GRANITE), groupData.get(1).get(0));
    }
}
