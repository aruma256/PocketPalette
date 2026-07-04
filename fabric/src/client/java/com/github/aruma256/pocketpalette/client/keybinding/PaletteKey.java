package com.github.aruma256.pocketpalette.client.keybinding;

import java.util.Deque;
import java.util.LinkedList;
import java.util.List;

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
import net.minecraft.world.item.ItemStack;

@Environment(EnvType.CLIENT)
public class PaletteKey extends KeyBase implements ScrollListener, RenderHotbarListener {

	protected final Deque<ItemStack> candidates = new LinkedList<>();
	protected int lastRotateTime = -1;
	protected byte rotateDirection = 0;

	private int phase = 0;

	public PaletteKey(int keyCode, String category) {
		super("key.pocketpalette.palette", keyCode, category);
	}

	private void updatePhase() {
		int maxGroups = ItemPalette.getGroupCount();
		if (maxGroups <= 2) {
			// Original behavior for 0-2 groups
			if (this.doubleTapTick == 0) {
				phase = 0;
			} else {
				phase ^= 1;
			}
		} else {
			// Cycle through all groups
			if (this.doubleTapTick == 0) {
				phase = 0;
			} else {
				phase = (phase + 1) % maxGroups;
			}
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
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || mc.player.isSpectator()) {
			return;
		}
		ItemStack itemStack = mc.player.getInventory().getSelectedItem();
		if (itemStack.isEmpty()) {
			return;
		}
		List<ItemStack> results = ItemPalette.getAllCycleItems(itemStack, getGroupIndex());
		if (results == null || results.size() <= 1) {
			return;
		}
		// Unlike Creative-based palettes, we cannot conjure items out of thin
		// air — only cycle entries that actually exist in the inventory can be
		// swapped in. The current item (index 0) is always a candidate.
		candidates.add(results.get(0));
		for (int i = 1; i < results.size(); i++) {
			if (InventorySwapper.findSlotWithItem(mc.player, results.get(i)) != -1) {
				candidates.add(results.get(i));
			}
		}
		if (candidates.size() <= 1) {
			candidates.clear();
		}
	}

	@Override
	protected void onKeyReleased() {
		candidates.clear();
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
		ItemStack target = candidates.getFirst();
		ItemStack selected = mc.player.getInventory().getSelectedItem();
		if (ItemStack.isSameItemSameComponents(selected, target)) {
			return; // already holding it (e.g. rotated back to the start)
		}
		int slot = InventorySwapper.findSlotWithItem(mc.player, target);
		if (slot == -1) {
			return; // the item disappeared from the inventory (consumed/moved)
		}
		InventorySwapper.swapWithSelected(slot);
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
			return;
		}
		if (candidates.isEmpty()) {
			return;
		}
		Minecraft mc = Minecraft.getInstance();
		int x = mc.getWindow().getGuiScaledWidth() / 2 - 90 + mc.player.getInventory().getSelectedSlot() * 20 + 2;
		int y = mc.getWindow().getGuiScaledHeight() - 16 - 3;
		y -= 50 + (20 + candidates.size());
		ItemStackRenderer.renderItemStacks(event.getGuiGraphics(), candidates, x, y, pressTime, event.getPartialTicks(), lastRotateTime, rotateDirection);
	}

}
