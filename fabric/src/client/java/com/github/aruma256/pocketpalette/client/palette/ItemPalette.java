package com.github.aruma256.pocketpalette.client.palette;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.github.aruma256.pocketpalette.PocketPalette;

import net.minecraft.world.item.ItemStack;

public class ItemPalette {

    // Cache for lookups: each group index maps to a Map<ItemState, List<ItemState>>
    private static final List<Map<ItemState, List<ItemState>>> GROUP_CACHES = new ArrayList<>();

    // Raw group data for serialization
    private static List<List<List<ItemState>>> groupData = new ArrayList<>();

    public static int getGroupCount() {
        return GROUP_CACHES.size();
    }

    /**
     * Get the lookup cache for the given group, or null if the stack is
     * invalid or the group index is out of range.
     */
    private static Map<ItemState, List<ItemState>> getCache(ItemStack itemStack, int groupIndex) {
        if (!ItemValidation.isValidStack(itemStack)) {
            return null;
        }
        if (groupIndex < 0 || groupIndex >= GROUP_CACHES.size()) {
            return null;
        }
        return GROUP_CACHES.get(groupIndex);
    }

    /**
     * Return the configured cycle entries in rotation order, starting with the
     * entry matching the queried stack. Exact component matches take priority
     * over componentless base-item matches. The returned entries retain their
     * configured components for inventory matching.
     *
     * @return the rotated cycle, or null for an invalid stack, group, or no match
     */
    public static List<ItemState> getCycleEntries(ItemStack itemStack, int groupIndex) {
        Map<ItemState, List<ItemState>> cache = getCache(itemStack, groupIndex);
        if (cache == null) {
            return null;
        }

        ItemState key = new ItemState(itemStack);

        // Stage 1: Try exact match (with components)
        List<ItemState> cycle = cache.get(key);

        // Stage 2: Fallback without components
        if (cycle == null) {
            key = key.withoutComponents();
            cycle = cache.get(key);
        }

        if (cycle == null) {
            return null;
        }

        return buildCycleEntries(cycle, key);
    }

    private static List<ItemState> buildCycleEntries(List<ItemState> cycle, ItemState startKey) {
        List<ItemState> result = new ArrayList<>();

        // Key not found in cycle shouldn't happen, but handle gracefully (start at 0)
        int startIndex = Math.max(0, cycle.indexOf(startKey));

        for (int i = 0; i < cycle.size(); i++) {
            int index = (startIndex + i) % cycle.size();
            result.add(cycle.get(index));
        }

        return result;
    }

    public static void loadGroups(List<List<List<ItemState>>> groups) {
        groupData = groups;
        GROUP_CACHES.clear();

        for (List<List<ItemState>> cyclesInGroup : groups) {
            Map<ItemState, List<ItemState>> cache = new HashMap<>();

            for (List<ItemState> cycle : cyclesInGroup) {
                if (cycle.size() < 2) {
                    continue;
                }

                // Register each item in the cycle
                for (ItemState itemState : cycle) {
                    if (cache.containsKey(itemState)) {
                        PocketPalette.LOGGER.warn("Duplicate item in palette: {}", itemState);
                        continue;
                    }
                    cache.put(itemState, cycle);
                }
            }

            GROUP_CACHES.add(cache);
        }
    }

    public static List<List<List<ItemState>>> getGroupData() {
        return groupData;
    }

    public static void clear() {
        GROUP_CACHES.clear();
        groupData = new ArrayList<>();
    }
}
