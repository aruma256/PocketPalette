package com.github.aruma256.pocketpalette.client.palette;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.world.item.ItemStack;

/**
 * GUI-independent editing session for the palette editor screen.
 *
 * For each group, resolves the cycle that contains the seed item (exact
 * component match first, then base item — same 2-stage lookup as
 * {@link ItemPalette}). If the item is not registered in a group, the
 * session starts a new cycle seeded with the base item.
 *
 * All mutations happen on working copies; nothing is written back to the
 * group data until {@link #commit()} is called.
 */
public class PaletteEditModel {

    public enum AddResult {
        ADDED,
        DUPLICATE_IN_CYCLE,
        DUPLICATE_IN_GROUP,
        INVALID,
    }

    private final List<List<List<ItemState>>> groupData;
    private final int groupCount;
    private final int[] originalCycleIndices; // -1 = new cycle
    private final List<List<ItemState>> workingCycles = new ArrayList<>();

    public PaletteEditModel(List<List<List<ItemState>>> groupData, ItemStack seedStack) {
        this.groupData = groupData;
        this.groupCount = Math.max(2, groupData.size());
        this.originalCycleIndices = new int[groupCount];

        ItemState exact = new ItemState(seedStack);
        ItemState base = exact.withoutComponents();

        for (int g = 0; g < groupCount; g++) {
            List<List<ItemState>> cycles = g < groupData.size() ? groupData.get(g) : List.of();
            int found = findCycleIndex(cycles, exact);
            if (found == -1 && !exact.equals(base)) {
                found = findCycleIndex(cycles, base);
            }
            originalCycleIndices[g] = found;

            List<ItemState> working = new ArrayList<>();
            if (found >= 0) {
                working.addAll(cycles.get(found));
            } else {
                working.add(base);
            }
            workingCycles.add(working);
        }
    }

    private static int findCycleIndex(List<List<ItemState>> cycles, ItemState itemState) {
        for (int i = 0; i < cycles.size(); i++) {
            if (cycles.get(i).contains(itemState)) {
                return i;
            }
        }
        return -1;
    }

    public int getGroupCount() {
        return groupCount;
    }

    public boolean isNewCycle(int groupIndex) {
        return originalCycleIndices[groupIndex] == -1;
    }

    public List<ItemState> getCycle(int groupIndex) {
        return Collections.unmodifiableList(workingCycles.get(groupIndex));
    }

    /**
     * Add the base item (components stripped) of the given stack to the
     * working cycle of the given group.
     */
    public AddResult add(int groupIndex, ItemStack itemStack) {
        if (!ItemValidation.isValidStack(itemStack)) {
            return AddResult.INVALID;
        }
        ItemState base = new ItemState(itemStack).withoutComponents();
        List<ItemState> working = workingCycles.get(groupIndex);
        if (working.contains(base)) {
            return AddResult.DUPLICATE_IN_CYCLE;
        }
        if (existsInOtherCycles(groupIndex, base)) {
            return AddResult.DUPLICATE_IN_GROUP;
        }
        working.add(base);
        return AddResult.ADDED;
    }

    private boolean existsInOtherCycles(int groupIndex, ItemState itemState) {
        if (groupIndex >= groupData.size()) {
            return false;
        }
        List<List<ItemState>> cycles = groupData.get(groupIndex);
        for (int i = 0; i < cycles.size(); i++) {
            if (i == originalCycleIndices[groupIndex]) {
                continue;
            }
            if (cycles.get(i).contains(itemState)) {
                return true;
            }
        }
        return false;
    }

    public boolean moveUp(int groupIndex, int index) {
        List<ItemState> working = workingCycles.get(groupIndex);
        if (index <= 0 || index >= working.size()) {
            return false;
        }
        Collections.swap(working, index, index - 1);
        return true;
    }

    public boolean moveDown(int groupIndex, int index) {
        List<ItemState> working = workingCycles.get(groupIndex);
        if (index < 0 || index >= working.size() - 1) {
            return false;
        }
        Collections.swap(working, index, index + 1);
        return true;
    }

    public boolean remove(int groupIndex, int index) {
        List<ItemState> working = workingCycles.get(groupIndex);
        if (index < 0 || index >= working.size()) {
            return false;
        }
        working.remove(index);
        return true;
    }

    /**
     * True if closing the editor now would discard an existing cycle in the
     * given group because it has been reduced to fewer than 2 items.
     */
    public boolean willDiscardCycle(int groupIndex) {
        return !isNewCycle(groupIndex) && workingCycles.get(groupIndex).size() < 2;
    }

    /**
     * Write the working cycles back into the group data.
     * Cycles with fewer than 2 items are removed (existing) or not added (new).
     *
     * @return true if the group data was modified
     */
    public boolean commit() {
        boolean changed = false;
        for (int g = 0; g < groupCount; g++) {
            while (groupData.size() <= g) {
                groupData.add(new ArrayList<>());
            }
            List<List<ItemState>> cycles = groupData.get(g);
            List<ItemState> working = workingCycles.get(g);
            int index = originalCycleIndices[g];
            if (index >= 0) {
                if (working.size() < 2) {
                    cycles.remove(index);
                    changed = true;
                } else if (!working.equals(cycles.get(index))) {
                    cycles.set(index, working);
                    changed = true;
                }
            } else if (working.size() >= 2) {
                cycles.add(working);
                changed = true;
            }
        }
        return changed;
    }
}
