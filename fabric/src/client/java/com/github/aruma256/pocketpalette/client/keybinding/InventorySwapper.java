package com.github.aruma256.pocketpalette.client.keybinding;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.IntFunction;
import java.util.function.Predicate;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;

/**
 * Swaps items between the selected hotbar slot and another inventory slot
 * by sending a SWAP container click (works in Survival, where inventory
 * slots cannot be overwritten directly).
 */
@Environment(EnvType.CLIENT)
public final class InventorySwapper {

	private InventorySwapper() {}

	/**
	 * Find all inventory slots (0-35, excluding the selected hotbar slot)
	 * containing an item that matches the target stack, ordered by stack
	 * count descending (ties keep slot order).
	 * Tries an exact component match first; if the target has no custom
	 * components and no exact match exists, falls back to matching by
	 * base item only.
	 *
	 * @return matching inventory slot indices, empty if none
	 */
	public static List<Integer> findSlotsWithItem(Player player, ItemStack target) {
		Inventory inventory = player.getInventory();
		return findMatchingSlots(inventory::getItem, inventory.getSelectedSlot(), target);
	}

	// Visible for testing: same contract as findSlotsWithItem, but reads slots
	// through a function so no Player/Inventory instance is required
	static List<Integer> findMatchingSlots(IntFunction<ItemStack> slotGetter, int selectedSlot, ItemStack target) {
		// Stage 1: exact match (item + components)
		List<Integer> exactMatches = collectMatches(slotGetter, selectedSlot,
				stack -> ItemStack.isSameItemSameComponents(stack, target));
		if (!exactMatches.isEmpty()) {
			return exactMatches;
		}

		// Stage 2: fallback to base item match, only when the target itself
		// has no custom components (mirrors ItemPalette's 2-stage lookup)
		if (target.getComponentsPatch().isEmpty()) {
			return collectMatches(slotGetter, selectedSlot,
					stack -> !stack.isEmpty() && stack.is(target.getItem()));
		}

		return List.of();
	}

	private static List<Integer> collectMatches(IntFunction<ItemStack> slotGetter, int selectedSlot, Predicate<ItemStack> matcher) {
		List<Integer> slots = new ArrayList<>();
		for (int i = 0; i < Inventory.INVENTORY_SIZE; i++) {
			if (i == selectedSlot) {
				continue;
			}
			if (matcher.test(slotGetter.apply(i))) {
				slots.add(i);
			}
		}
		// Fullest stack first, so one scroll reaches the most useful stack
		slots.sort(Comparator.comparingInt((Integer slot) -> slotGetter.apply(slot).getCount()).reversed());
		return slots;
	}

	/**
	 * Swap the given inventory slot (0-35) with the selected hotbar slot.
	 * Sends a SWAP click through the player's InventoryMenu, so the server
	 * performs the swap — no creative permissions required.
	 *
	 * @return true if the SWAP click was sent
	 */
	public static boolean swapWithSelected(int inventorySlot) {
		Minecraft mc = Minecraft.getInstance();
		LocalPlayer player = mc.player;
		if (player == null || mc.gameMode == null) {
			return false;
		}
		if (player.containerMenu != player.inventoryMenu) {
			// Another container is open; a SWAP click would target the wrong menu
			return false;
		}
		int menuSlot = inventorySlotToMenuSlot(inventorySlot);
		int selectedHotbarSlot = player.getInventory().getSelectedSlot();
		mc.gameMode.handleInventoryMouseClick(
				player.inventoryMenu.containerId,
				menuSlot,
				selectedHotbarSlot,
				ClickType.SWAP,
				player);
		return true;
	}

	/**
	 * Convert an Inventory index (0-8 hotbar, 9-35 main) to the corresponding
	 * InventoryMenu slot index (36-44 hotbar, 9-35 main).
	 */
	static int inventorySlotToMenuSlot(int inventorySlot) {
		if (inventorySlot < Inventory.getSelectionSize()) {
			return InventoryMenu.USE_ROW_SLOT_START + inventorySlot;
		}
		return inventorySlot;
	}

}
