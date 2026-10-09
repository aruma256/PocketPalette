package com.github.aruma256.pocketpalette.client.keybinding;

import java.util.List;

import com.github.aruma256.pocketpalette.PocketPalette;
import com.github.aruma256.pocketpalette.client.compat.LotTweaksCompat;
import com.github.aruma256.pocketpalette.client.palette.ItemPalette;
import com.github.aruma256.pocketpalette.client.palette.ItemState;
import com.github.aruma256.pocketpalette.client.render.ItemStackRenderer;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

@Environment(EnvType.CLIENT)
public class PaletteKey extends KeyBase {

	// Vanilla raises the selected item name by 14 GUI pixels when the Survival HUD is shown.
	private static final int SURVIVAL_HUD_OFFSET = 14;

	private final PaletteSession session = new PaletteSession();
	private final LotTweaksCompat lotTweaksCompat = new LotTweaksCompat();
	protected int lastRotateTime = -1;
	protected byte rotateDirection = 0;

	// Revalidated before scrolling, since number keys can change the selected slot.
	private LocalPlayer observedPlayer;
	private boolean observedSpectator;

	private int phase = 0;

	public PaletteKey(int keyCode, Category category) {
		super("key.pocketpalette.palette", keyCode, category);
	}

	@Override
	public boolean isEnabled() {
		Minecraft client = Minecraft.getInstance();
		return client.player == null || !lotTweaksCompat.shouldDisablePaletteKey(
				client.player.isCreative(), this, client.options.keyMappings);
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
		session.clear();
		observedPlayer = Minecraft.getInstance().player;
		if (observedPlayer == null) {
			return;
		}
		observedSpectator = observedPlayer.isSpectator();
		Inventory inventory = observedPlayer.getInventory();
		List<ItemState> entries = observedSpectator ? null
				: ItemPalette.getCycleEntries(inventory.getSelectedItem(), getGroupIndex());
		session.rebuild(inventory::getItem, inventory.getSelectedSlot(), entries);
	}

	@Override
	protected void onKeyReleased() {
		session.clear();
		observedPlayer = null;
	}

	private void refreshCandidatesIfChanged() {
		LocalPlayer player = Minecraft.getInstance().player;
		if (player != observedPlayer) {
			rebuildCandidates();
			return;
		}
		if (player == null) return;
		Inventory inventory = player.getInventory();
		if (player.isSpectator() != observedSpectator
				|| session.hasInventoryChanged(inventory::getItem, inventory.getSelectedSlot())) {
			rebuildCandidates();
		}
	}

	@Override
	protected void whilePressed() {
		refreshCandidatesIfChanged();
	}

	protected void rotateCandidatesForward() {
		session.rotateForward();
		this.lastRotateTime = this.pressTime;
		this.rotateDirection = 1;
	}

	protected void rotateCandidatesBackward() {
		session.rotateBackward();
		this.lastRotateTime = this.pressTime;
		this.rotateDirection = -1;
	}

	private void swapToFirstCandidate() {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null) {
			return;
		}
		session.swapToFirstCandidate(mc.player.getInventory()::getItem, InventorySwapper::swapWithSelected);
	}

	/** Returns whether palette input consumes the scroll, even without candidates. */
	public boolean onScroll(double wheel) {
		if (!isEnabled()) {
			resetInputState();
			return false;
		}
		if (this.pressTime == 0 || wheel == 0) {
			return false;
		}
		// Input callbacks can arrive between ticks, so validate here too.
		refreshCandidatesIfChanged();
		if (session.isEmpty()) {
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

	public void onRenderHotbar(GuiGraphicsExtractor guiGraphics, float partialTicks) {
		if (!isEnabled()) {
			resetInputState();
			return;
		}
		if (this.pressTime == 0) {
			session.clear();
			return;
		}
		if (session.isEmpty()) {
			return;
		}
		Minecraft mc = Minecraft.getInstance();
		int x = mc.getWindow().getGuiScaledWidth() / 2 - 90 + mc.player.getInventory().getSelectedSlot() * 20 + 2;
		int y = mc.getWindow().getGuiScaledHeight() - 16 - 3;
		y -= 50 + (20 + session.size()) + SURVIVAL_HUD_OFFSET;
		List<ItemStack> stacks = session.getStacks();
		ItemStackRenderer.renderItemStacks(guiGraphics, stacks, x, y, pressTime, partialTicks, lastRotateTime, rotateDirection);
	}

}
