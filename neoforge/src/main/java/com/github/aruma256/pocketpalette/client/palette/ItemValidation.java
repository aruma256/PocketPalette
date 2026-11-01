package com.github.aruma256.pocketpalette.client.palette;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class ItemValidation {

    private ItemValidation() {}

    public static boolean isValidStack(ItemStack itemStack) {
        if (itemStack == null || itemStack.isEmpty()) {
            return false;
        }
        return isValidItem(itemStack.getItem());
    }

    public static boolean isValidItem(Item item) {
        return item != null && item != Items.AIR;
    }
}
