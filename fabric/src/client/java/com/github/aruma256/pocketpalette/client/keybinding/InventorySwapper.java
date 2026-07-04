package com.github.aruma256.pocketpalette.client.keybinding;

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
	 * Find an inventory slot (0-35, excluding the selected hotbar slot)
	 * containing an item that matches the target stack.
	 * Tries an exact component match first; if the target has no custom
	 * components, falls back to matching by base item only.
	 *
	 * @return the inventory slot index, or -1 if not found
	 */
	public static int findSlotWithItem(Player player, ItemStack target) {
		Inventory inventory = player.getInventory();
		int selectedSlot = inventory.getSelectedSlot();

		// Stage 1: exact match (item + components)
		for (int i = 0; i < Inventory.INVENTORY_SIZE; i++) {
			if (i == selectedSlot) {
				continue;
			}
			if (ItemStack.isSameItemSameComponents(inventory.getItem(i), target)) {
				return i;
			}
		}

		// Stage 2: fallback to base item match, only when the target itself
		// has no custom components (mirrors ItemPalette's 2-stage lookup)
		if (target.getComponentsPatch().isEmpty()) {
			for (int i = 0; i < Inventory.INVENTORY_SIZE; i++) {
				if (i == selectedSlot) {
					continue;
				}
				ItemStack stack = inventory.getItem(i);
				if (!stack.isEmpty() && stack.is(target.getItem())) {
					return i;
				}
			}
		}

		return -1;
	}

	/**
	 * Swap the given inventory slot (0-35) with the selected hotbar slot.
	 * Sends a SWAP click through the player's InventoryMenu, so the server
	 * performs the swap — no creative permissions required.
	 */
	public static void swapWithSelected(int inventorySlot) {
		Minecraft mc = Minecraft.getInstance();
		LocalPlayer player = mc.player;
		if (player == null || mc.gameMode == null) {
			return;
		}
		if (player.containerMenu != player.inventoryMenu) {
			// Another container is open; a SWAP click would target the wrong menu
			return;
		}
		int menuSlot = inventorySlotToMenuSlot(inventorySlot);
		int selectedHotbarSlot = player.getInventory().getSelectedSlot();
		mc.gameMode.handleInventoryMouseClick(
				player.inventoryMenu.containerId,
				menuSlot,
				selectedHotbarSlot,
				ClickType.SWAP,
				player);
	}

	/**
	 * Convert an Inventory index (0-8 hotbar, 9-35 main) to the corresponding
	 * InventoryMenu slot index (36-44 hotbar, 9-35 main).
	 */
	private static int inventorySlotToMenuSlot(int inventorySlot) {
		if (inventorySlot < Inventory.getSelectionSize()) {
			return InventoryMenu.USE_ROW_SLOT_START + inventorySlot;
		}
		return inventorySlot;
	}

}
