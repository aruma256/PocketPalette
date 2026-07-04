package com.github.aruma256.pocketpalette.client.palette;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;
import java.util.Map;

import com.github.aruma256.pocketpalette.MinecraftBootstrapExtension;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

@ExtendWith(MinecraftBootstrapExtension.class)
public class ItemStateTest {

    @Test
    public void testEqualsWithSameItem() {
        ItemState state1 = new ItemState(new ItemStack(Items.STONE));
        ItemState state2 = new ItemState(new ItemStack(Items.STONE));
        assertEquals(state1, state2);
    }

    @Test
    public void testNotEqualsWithDifferentItem() {
        ItemState state1 = new ItemState(new ItemStack(Items.STONE));
        ItemState state2 = new ItemState(new ItemStack(Items.GRANITE));
        assertNotEquals(state1, state2);
    }

    @Test
    public void testEqualsWithSameCustomName() {
        ItemStack stack1 = new ItemStack(Items.DIAMOND_SWORD);
        stack1.set(DataComponents.CUSTOM_NAME, Component.literal("Fire Sword"));

        ItemStack stack2 = new ItemStack(Items.DIAMOND_SWORD);
        stack2.set(DataComponents.CUSTOM_NAME, Component.literal("Fire Sword"));

        ItemState state1 = new ItemState(stack1);
        ItemState state2 = new ItemState(stack2);
        assertEquals(state1, state2);
    }

    @Test
    public void testNotEqualsWithDifferentCustomName() {
        ItemStack stack1 = new ItemStack(Items.DIAMOND_SWORD);
        stack1.set(DataComponents.CUSTOM_NAME, Component.literal("Fire Sword"));

        ItemStack stack2 = new ItemStack(Items.DIAMOND_SWORD);
        stack2.set(DataComponents.CUSTOM_NAME, Component.literal("Ice Sword"));

        ItemState state1 = new ItemState(stack1);
        ItemState state2 = new ItemState(stack2);
        assertNotEquals(state1, state2);
    }

    @Test
    public void testNotEqualsWithVsWithoutComponents() {
        ItemStack plainStack = new ItemStack(Items.DIAMOND_SWORD);

        ItemStack namedStack = new ItemStack(Items.DIAMOND_SWORD);
        namedStack.set(DataComponents.CUSTOM_NAME, Component.literal("Named Sword"));

        ItemState plainState = new ItemState(plainStack);
        ItemState namedState = new ItemState(namedStack);
        assertNotEquals(plainState, namedState);
    }

    @Test
    public void testHashCodeConsistency() {
        ItemStack stack1 = new ItemStack(Items.DIAMOND_SWORD);
        stack1.set(DataComponents.CUSTOM_NAME, Component.literal("Fire Sword"));

        ItemStack stack2 = new ItemStack(Items.DIAMOND_SWORD);
        stack2.set(DataComponents.CUSTOM_NAME, Component.literal("Fire Sword"));

        ItemState state1 = new ItemState(stack1);
        ItemState state2 = new ItemState(stack2);

        assertEquals(state1.hashCode(), state2.hashCode());
    }

    @Test
    public void testHashMapBehavior() {
        ItemStack namedStack = new ItemStack(Items.DIAMOND_SWORD);
        namedStack.set(DataComponents.CUSTOM_NAME, Component.literal("Fire Sword"));

        ItemState key1 = new ItemState(namedStack);
        ItemState key2 = new ItemState(namedStack.copy());

        Map<ItemState, String> map = new HashMap<>();
        map.put(key1, "value");

        // Should find the value using an equal but different instance
        assertEquals("value", map.get(key2));
    }

    @Test
    public void testHashMapDistinguishesComponents() {
        ItemStack plainStack = new ItemStack(Items.DIAMOND_SWORD);

        ItemStack namedStack = new ItemStack(Items.DIAMOND_SWORD);
        namedStack.set(DataComponents.CUSTOM_NAME, Component.literal("Named"));

        Map<ItemState, String> map = new HashMap<>();
        map.put(new ItemState(plainStack), "plain");
        map.put(new ItemState(namedStack), "named");

        assertEquals(2, map.size());
        assertEquals("plain", map.get(new ItemState(new ItemStack(Items.DIAMOND_SWORD))));
    }

    @Test
    public void testWithoutComponents() {
        ItemStack namedStack = new ItemStack(Items.DIAMOND_SWORD);
        namedStack.set(DataComponents.CUSTOM_NAME, Component.literal("Named Sword"));

        ItemState namedState = new ItemState(namedStack);
        ItemState strippedState = namedState.withoutComponents();

        assertTrue(namedState.hasComponents());
        assertFalse(strippedState.hasComponents());

        ItemState plainState = new ItemState(new ItemStack(Items.DIAMOND_SWORD));
        assertEquals(plainState, strippedState);
    }

    @Test
    public void testCountNormalization() {
        ItemStack stack1 = new ItemStack(Items.STONE, 1);
        ItemStack stack64 = new ItemStack(Items.STONE, 64);

        ItemState state1 = new ItemState(stack1);
        ItemState state64 = new ItemState(stack64);

        assertEquals(state1, state64);
        assertEquals(state1.hashCode(), state64.hashCode());
    }

    @Test
    public void testToItemStackReturnsCopy() {
        ItemStack original = new ItemStack(Items.STONE);
        ItemState state = new ItemState(original);

        ItemStack returned = state.toItemStack();
        returned.setCount(64);

        // Modifying returned copy shouldn't affect internal state
        ItemStack returned2 = state.toItemStack();
        assertEquals(1, returned2.getCount());
    }

    @Test
    public void testConstructorMakesCopy() {
        ItemStack original = new ItemStack(Items.STONE);
        ItemState state = new ItemState(original);

        original.set(DataComponents.CUSTOM_NAME, Component.literal("Modified"));

        // Modifying original shouldn't affect the ItemState
        ItemState plainState = new ItemState(new ItemStack(Items.STONE));
        assertEquals(plainState, state);
    }

    @Test
    public void testHasComponentsReturnsFalseForPlainItem() {
        ItemState state = new ItemState(new ItemStack(Items.STONE));
        assertFalse(state.hasComponents());
    }

    @Test
    public void testHasComponentsReturnsTrueForNamedItem() {
        ItemStack namedStack = new ItemStack(Items.STONE);
        namedStack.set(DataComponents.CUSTOM_NAME, Component.literal("Named Stone"));

        ItemState state = new ItemState(namedStack);
        assertTrue(state.hasComponents());
    }
}
