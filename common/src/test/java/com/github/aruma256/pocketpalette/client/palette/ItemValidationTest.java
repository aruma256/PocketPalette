package com.github.aruma256.pocketpalette.client.palette;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;

import com.github.aruma256.pocketpalette.MinecraftBootstrapExtension;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

@ExtendWith(MinecraftBootstrapExtension.class)
public class ItemValidationTest {

    @Test
    public void testIsValidStack_withNull() {
        assertFalse(ItemValidation.isValidStack(null));
    }

    @Test
    public void testIsValidStack_withEmptyStack() {
        assertFalse(ItemValidation.isValidStack(ItemStack.EMPTY));
    }

    @Test
    public void testIsValidStack_withAirStack() {
        assertFalse(ItemValidation.isValidStack(new ItemStack(Items.AIR)));
    }

    @Test
    public void testIsValidStack_withValidStack() {
        assertTrue(ItemValidation.isValidStack(new ItemStack(Items.STONE)));
        assertTrue(ItemValidation.isValidStack(new ItemStack(Items.DIAMOND_SWORD)));
    }

    @Test
    public void testIsValidItem_withNull() {
        assertFalse(ItemValidation.isValidItem(null));
    }

    @Test
    public void testIsValidItem_withAir() {
        assertFalse(ItemValidation.isValidItem(Items.AIR));
    }

    @Test
    public void testIsValidItem_withValidItem() {
        assertTrue(ItemValidation.isValidItem(Items.STONE));
        assertTrue(ItemValidation.isValidItem(Items.DIAMOND_SWORD));
    }
}
