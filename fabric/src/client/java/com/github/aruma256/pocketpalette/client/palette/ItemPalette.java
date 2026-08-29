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

    public static boolean canCycle(ItemStack itemStack, int groupIndex) {
        Map<ItemState, List<ItemState>> cache = getCache(itemStack, groupIndex);
        if (cache == null) {
            return false;
        }
        ItemState key = new ItemState(itemStack);
        // Try exact match first, then fallback without components
        return cache.containsKey(key) || cache.containsKey(key.withoutComponents());
    }

    /**
     * Check if an exact item (with its specific components) is already registered.
     * Unlike canCycle(), this does NOT use fallback search.
     * Use this for duplicate detection when adding new items.
     */
    public static boolean containsExactItem(ItemStack itemStack, int groupIndex) {
        Map<ItemState, List<ItemState>> cache = getCache(itemStack, groupIndex);
        return cache != null && cache.containsKey(new ItemState(itemStack));
    }

    public static List<ItemStack> getAllCycleItems(ItemStack itemStack, int groupIndex) {
        List<ItemState> entries = getCycleEntries(itemStack, groupIndex);
        if (entries == null) {
            return null;
        }

        List<ItemStack> result = new ArrayList<>();

        // Preserve the actual held stack for display, even when it matched a
        // componentless palette entry through fallback.
        result.add(itemStack);

        for (int i = 1; i < entries.size(); i++) {
            result.add(entries.get(i).toItemStack());
        }

        return result;
    }

    /**
     * Return the configured cycle entries in rotation order. Unlike
     * {@link #getAllCycleItems(ItemStack, int)}, the first element is the
     * palette entry that matched the queried stack, not the queried stack
     * itself. This preserves whether inventory matching should use an exact
     * component match or a componentless base-item match.
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

    public static void clearGroup(int groupIndex) {
        if (groupIndex >= 0 && groupIndex < GROUP_CACHES.size()) {
            GROUP_CACHES.get(groupIndex).clear();
        }
        if (groupIndex >= 0 && groupIndex < groupData.size()) {
            groupData.get(groupIndex).clear();
        }
    }
}
