package com.github.aruma256.pocketpalette.client.keybinding;

import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import com.github.aruma256.pocketpalette.PocketPalette;
import com.github.aruma256.pocketpalette.client.event.RenderHotbarEvent;
import com.github.aruma256.pocketpalette.client.event.RenderHotbarEvent.RenderHotbarListener;
import com.github.aruma256.pocketpalette.client.event.ScrollEvent;
import com.github.aruma256.pocketpalette.client.event.ScrollEvent.ScrollListener;
import com.github.aruma256.pocketpalette.client.palette.ItemPalette;
import com.github.aruma256.pocketpalette.client.render.ItemStackRenderer;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

@Environment(EnvType.CLIENT)
public class PaletteKey extends KeyBase implements ScrollListener, RenderHotbarListener {

	/**
	 * One ring entry = one physical inventory stack. {@code slot} tracks
	 * where the stack currently sits; swaps exchange the slot values of the
	 * two candidates involved, so the ring stays consistent while rotating.
	 */
	static final class Candidate {
		final ItemStack stack;
		int slot;

		Candidate(ItemStack stack, int slot) {
			this.stack = stack;
			this.slot = slot;
		}
	}

	protected final Deque<Candidate> candidates = new LinkedList<>();
	protected int lastRotateTime = -1;
	protected byte rotateDirection = 0;

	// The candidate currently in the selected hotbar slot (invariant:
	// handCandidate.slot == selected slot, which cannot change while the
	// palette key is held because scroll events are intercepted)
	private Candidate handCandidate;

	private int phase = 0;

	public PaletteKey(int keyCode, String category) {
		super("key.pocketpalette.palette", keyCode, category);
	}

	private void updatePhase() {
		if (this.doubleTapTick == 0) {
			phase = 0;
		} else {
			// Double-tap cycles through all groups (toggle 0/1 when 2 or fewer)
			phase = (phase + 1) % Math.max(2, ItemPalette.getGroupCount());
		}
	}

	private int getGroupIndex() {
		int maxGroups = ItemPalette.getGroupCount();
		if (maxGroups == 0) return 0;

		if (PocketPalette.CONFIG.SNEAK_TO_SWITCH_GROUP) {
			return Minecraft.getInstance().player.isShiftKeyDown() ? 1 : 0;
		} else {
			return this.phase % maxGroups;
		}
	}

	@Override
	protected void onKeyPressStart() {
		this.lastRotateTime = 0;
		this.rotateDirection = 0;
		this.updatePhase();
		candidates.clear();
		handCandidate = null;
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || mc.player.isSpectator()) {
			return;
		}
		Inventory inventory = mc.player.getInventory();
		ItemStack itemStack = inventory.getSelectedItem();
		if (itemStack.isEmpty()) {
			return;
		}
		List<ItemStack> results = ItemPalette.getAllCycleItems(itemStack, getGroupIndex());
		if (results == null || results.isEmpty()) {
			return;
		}
		// Unlike Creative-based palettes, we cannot conjure items out of thin
		// air — the ring offers the actual inventory stacks, one candidate per
		// stack, in cycle order. The held stack always leads; other stacks of
		// the same item follow it, which doubles as a restock ("swap in a full
		// stack of what I'm holding").
		candidates.add(new Candidate(itemStack.copy(), inventory.getSelectedSlot()));
		Set<Integer> claimedSlots = new HashSet<>();
		for (ItemStack result : results) {
			for (int slot : InventorySwapper.findSlotsWithItem(mc.player, result)) {
				// A slot may match more than one cycle entry (base-item
				// fallback); each physical stack appears only once
				if (!claimedSlots.add(slot)) {
					continue;
				}
				candidates.add(new Candidate(inventory.getItem(slot).copy(), slot));
			}
		}
		if (candidates.size() <= 1) {
			candidates.clear();
			return;
		}
		handCandidate = candidates.getFirst();
	}

	@Override
	protected void onKeyReleased() {
		candidates.clear();
		handCandidate = null;
	}

	protected void rotateCandidatesForward() {
		candidates.addFirst(candidates.pollLast());
		this.lastRotateTime = this.pressTime;
		this.rotateDirection = 1;
	}

	protected void rotateCandidatesBackward() {
		candidates.addLast(candidates.pollFirst());
		this.lastRotateTime = this.pressTime;
		this.rotateDirection = -1;
	}

	private void swapToFirstCandidate() {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null) {
			return;
		}
		Candidate target = candidates.getFirst();
		if (target == handCandidate) {
			return; // rotated back to the stack already in hand
		}
		ItemStack inSlot = mc.player.getInventory().getItem(target.slot);
		if (!ItemStack.isSameItemSameComponents(inSlot, target.stack)) {
			return; // the stack moved or was consumed since the key was pressed
		}
		if (!InventorySwapper.swapWithSelected(target.slot)) {
			return;
		}
		// The stacks traded places: the previous hand stack now sits in the
		// target's old slot, and the target is in the selected hotbar slot
		int targetSlot = target.slot;
		target.slot = handCandidate.slot;
		handCandidate.slot = targetSlot;
		handCandidate = target;
	}

	@Override
	public void onScroll(ScrollEvent event) {
		if (this.pressTime == 0) {
			return;
		}
		if (event.isCanceled()) {
			return;
		}
		double wheel = event.getScrollDelta();
		if (wheel == 0) {
			return;
		}
		event.setCanceled(true);
		if (candidates.isEmpty()) {
			return;
		}
		if (wheel > 0) {
			this.rotateCandidatesForward();
		} else {
			this.rotateCandidatesBackward();
		}
		this.swapToFirstCandidate();
	}

	@Override
	public void onRenderHotbar(RenderHotbarEvent event) {
		if (this.pressTime == 0) {
			candidates.clear();
			handCandidate = null;
			return;
		}
		if (candidates.isEmpty()) {
			return;
		}
		Minecraft mc = Minecraft.getInstance();
		int x = mc.getWindow().getGuiScaledWidth() / 2 - 90 + mc.player.getInventory().getSelectedSlot() * 20 + 2;
		int y = mc.getWindow().getGuiScaledHeight() - 16 - 3;
		y -= 50 + (20 + candidates.size());
		List<ItemStack> stacks = candidates.stream().map(candidate -> candidate.stack).toList();
		ItemStackRenderer.renderItemStacks(event.getGuiGraphics(), stacks, x, y, pressTime, event.getPartialTicks(), lastRotateTime, rotateDirection);
	}

}
