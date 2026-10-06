package com.github.aruma256.pocketpalette.client.keybinding;

import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import com.github.aruma256.pocketpalette.PocketPalette;
import com.github.aruma256.pocketpalette.client.palette.ItemPalette;
import com.github.aruma256.pocketpalette.client.palette.ItemState;
import com.github.aruma256.pocketpalette.client.render.ItemStackRenderer;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

@Environment(EnvType.CLIENT)
public class PaletteKey extends KeyBase {

	// Vanilla raises the selected item name by 14 GUI pixels when the Survival HUD is shown.
	private static final int SURVIVAL_HUD_OFFSET = 14;

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

	// Revalidated before scrolling, since number keys can change the selected slot.
	private Candidate handCandidate;
	private LocalPlayer observedPlayer;
	private int observedSelectedSlot = -1;
	private boolean observedSpectator;
	private final ItemStack[] inventorySnapshot = new ItemStack[Inventory.INVENTORY_SIZE];

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
		this.updatePhase();
		rebuildCandidates();
	}

	private void rebuildCandidates() {
		this.pressTime = 1;
		this.lastRotateTime = 0;
		this.rotateDirection = 0;
		candidates.clear();
		handCandidate = null;
		Minecraft mc = Minecraft.getInstance();
		rememberInventory();
		if (mc.player == null || mc.player.isSpectator()) {
			return;
		}
		Inventory inventory = mc.player.getInventory();
		ItemStack itemStack = inventory.getSelectedItem();
		if (itemStack.isEmpty()) {
			return;
		}
		List<ItemState> entries = ItemPalette.getCycleEntries(itemStack, getGroupIndex());
		if (entries == null || entries.isEmpty()) {
			return;
		}
		// Unlike Creative-based palettes, we cannot conjure items out of thin
		// air — the ring offers the actual inventory stacks, one candidate per
		// stack, in cycle order. The held stack always leads; other stacks of
		// the same item follow it, ordered by count ascending, then remaining
		// durability ascending, so smaller stacks and worn tools come first.
		candidates.add(new Candidate(itemStack.copy(), inventory.getSelectedSlot()));
		Set<Integer> claimedSlots = new HashSet<>();
		for (ItemState entry : entries) {
			for (int slot : InventorySwapper.findSlotsMatchingPaletteEntry(mc.player, entry)) {
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
		observedPlayer = null;
		observedSelectedSlot = -1;
		java.util.Arrays.fill(inventorySnapshot, null);
	}

	private void rememberInventory() {
		observedPlayer = Minecraft.getInstance().player;
		if (observedPlayer == null) return;
		Inventory inventory = observedPlayer.getInventory();
		observedSelectedSlot = inventory.getSelectedSlot();
		observedSpectator = observedPlayer.isSpectator();
		for (int slot = 0; slot < inventorySnapshot.length; slot++) {
			inventorySnapshot[slot] = inventory.getItem(slot).copy();
		}
	}

	private void refreshCandidatesIfChanged() {
		LocalPlayer player = Minecraft.getInstance().player;
		if (player != observedPlayer) {
			rebuildCandidates();
			return;
		}
		if (player == null) return;
		Inventory inventory = player.getInventory();
		if (inventory.getSelectedSlot() != observedSelectedSlot || player.isSpectator() != observedSpectator) {
			rebuildCandidates();
			return;
		}
		// Compare all 36 slots, including non-candidates: commands, pickups and
		// consumption can add candidates as well as change or remove them.
		for (int slot = 0; slot < inventorySnapshot.length; slot++) {
			if (!ItemStack.matches(inventorySnapshot[slot], inventory.getItem(slot))) {
				rebuildCandidates();
				return;
			}
		}
	}

	@Override
	protected void whilePressed() {
		refreshCandidatesIfChanged();
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
		// The locally predicted SWAP is our own change; keep its ring order.
		// A later server correction will still differ from this snapshot.
		rememberInventory();
	}

	/** Returns whether palette input consumes the scroll, even without candidates. */
	public boolean onScroll(double wheel) {
		if (this.pressTime == 0 || wheel == 0) {
			return false;
		}
		// Input callbacks can arrive between ticks, so validate here too.
		refreshCandidatesIfChanged();
		if (candidates.isEmpty()) {
			return true;
		}
		if (wheel > 0) {
			this.rotateCandidatesBackward();
		} else {
			this.rotateCandidatesForward();
		}
		this.swapToFirstCandidate();
		return true;
	}

	public void onRenderHotbar(GuiGraphics guiGraphics, float partialTicks) {
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
		y -= 50 + (20 + candidates.size()) + SURVIVAL_HUD_OFFSET;
		List<ItemStack> stacks = candidates.stream().map(candidate -> candidate.stack).toList();
		ItemStackRenderer.renderItemStacks(guiGraphics, stacks, x, y, pressTime, partialTicks, lastRotateTime, rotateDirection);
	}

}
