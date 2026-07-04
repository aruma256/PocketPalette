package com.github.aruma256.pocketpalette.client.palette;

import net.minecraft.world.item.ItemStack;

/**
 * NBT/Component-aware wrapper for ItemStack that can be used as a HashMap key.
 * Two ItemStates are considered equal if they have the same item and components.
 * Stack count is normalized to 1 (count differences are ignored).
 */
public class ItemState {

    private final ItemStack cachedStack;

    public ItemState(ItemStack itemStack) {
        this.cachedStack = itemStack.copy();
        this.cachedStack.setCount(1);
    }

    public ItemStack toItemStack() {
        return this.cachedStack.copy();
    }

    public ItemState withoutComponents() {
        ItemStack stripped = new ItemStack(cachedStack.getItem());
        return new ItemState(stripped);
    }

    public boolean hasComponents() {
        return !cachedStack.getComponentsPatch().isEmpty();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof ItemState other)) return false;
        return ItemStack.matches(this.cachedStack, other.cachedStack);
    }

    @Override
    public int hashCode() {
        int hash = cachedStack.getItem().hashCode();
        if (!cachedStack.getComponentsPatch().isEmpty()) {
            hash = 31 * hash + cachedStack.getComponentsPatch().hashCode();
        }
        return hash;
    }

    @Override
    public String toString() {
        return "ItemState{" + cachedStack + "}";
    }
}
