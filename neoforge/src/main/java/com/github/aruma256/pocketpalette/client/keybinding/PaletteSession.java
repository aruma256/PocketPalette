package com.github.aruma256.pocketpalette.client.keybinding;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.IntFunction;
import java.util.function.IntPredicate;

import com.github.aruma256.pocketpalette.client.palette.ItemState;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/**
 * Candidate ring and inventory tracking for one held-key palette session.
 * Inventory reads and swaps are supplied by the caller; this class does not
 * access the Minecraft client or handle input and animation timing.
 */
final class PaletteSession {

	/** One candidate per physical stack; swaps exchange the candidates' slots. */
	private static final class Candidate {
		final ItemStack stack;
		int slot;

		Candidate(ItemStack stack, int slot) {
			this.stack = stack.copy();
			this.slot = slot;
		}
	}

	private final Deque<Candidate> candidates = new ArrayDeque<>();
	private Candidate handCandidate;
	private int observedSelectedSlot = -1;
	private final ItemStack[] inventorySnapshot = new ItemStack[Inventory.INVENTORY_SIZE];

	void rebuild(IntFunction<ItemStack> slotGetter, int selectedSlot, List<ItemState> entries) {
		clear();
		rememberInventory(slotGetter, selectedSlot);
		ItemStack heldStack = slotGetter.apply(selectedSlot);
		if (heldStack.isEmpty() || entries == null || entries.isEmpty()) {
			return;
		}
		// The held stack leads. Other stacks follow in cycle order, sorted by
		// count and remaining durability by InventorySwapper.
		candidates.add(new Candidate(heldStack, selectedSlot));
		Set<Integer> claimedSlots = new HashSet<>();
		for (ItemState entry : entries) {
			for (int slot : InventorySwapper.findMatchingSlots(slotGetter, selectedSlot, entry)) {
				// Base-item and exact entries can match the same physical stack.
				if (claimedSlots.add(slot)) {
					candidates.add(new Candidate(slotGetter.apply(slot), slot));
				}
			}
		}
		if (candidates.size() <= 1) {
			candidates.clear();
			return;
		}
		handCandidate = candidates.getFirst();
	}

	void clear() {
		candidates.clear();
		handCandidate = null;
		observedSelectedSlot = -1;
		Arrays.fill(inventorySnapshot, null);
	}

	boolean isEmpty() {
		return candidates.isEmpty();
	}

	int size() {
		return candidates.size();
	}

	/** Copies for rendering, so callers cannot mutate the tracked stacks. */
	List<ItemStack> getStacks() {
		return candidates.stream().map(candidate -> candidate.stack.copy()).toList();
	}

	boolean hasInventoryChanged(IntFunction<ItemStack> slotGetter, int selectedSlot) {
		if (selectedSlot != observedSelectedSlot) {
			return true;
		}
		// Include non-candidates: pickups and commands can add new candidates.
		for (int slot = 0; slot < inventorySnapshot.length; slot++) {
			if (!ItemStack.matches(inventorySnapshot[slot], slotGetter.apply(slot))) {
				return true;
			}
		}
		return false;
	}

	private void rememberInventory(IntFunction<ItemStack> slotGetter, int selectedSlot) {
		observedSelectedSlot = selectedSlot;
		for (int slot = 0; slot < inventorySnapshot.length; slot++) {
			inventorySnapshot[slot] = slotGetter.apply(slot).copy();
		}
	}

	void rotateForward() {
		if (!isEmpty()) candidates.addFirst(candidates.removeLast());
	}

	void rotateBackward() {
		if (!isEmpty()) candidates.addLast(candidates.removeFirst());
	}

	/**
	 * Try the caller's SWAP operation for the first candidate. Rotation is
	 * retained on failure, but hand and slot tracking change only on success.
	 * The operation must apply the local inventory prediction before returning.
	 */
	boolean swapToFirstCandidate(IntFunction<ItemStack> slotGetter, IntPredicate swapper) {
		if (isEmpty()) return false;
		Candidate target = candidates.getFirst();
		if (target == handCandidate
				|| !ItemStack.isSameItemSameComponents(slotGetter.apply(target.slot), target.stack)) {
			return false;
		}
		if (!swapper.test(target.slot)) {
			return false;
		}
		int targetSlot = target.slot;
		target.slot = handCandidate.slot;
		handCandidate.slot = targetSlot;
		handCandidate = target;
		// Preserve ring order for our predicted swap. A later server correction
		// still differs from this snapshot and causes the caller to rebuild.
		rememberInventory(slotGetter, observedSelectedSlot);
		return true;
	}
}
