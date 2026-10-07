package com.github.aruma256.pocketpalette.client.gui;

import java.util.List;

import com.github.aruma256.pocketpalette.client.PocketPaletteClient;
import com.github.aruma256.pocketpalette.client.palette.ItemPalette;
import com.github.aruma256.pocketpalette.client.palette.ItemState;
import com.github.aruma256.pocketpalette.client.palette.PaletteConfigManager;
import com.github.aruma256.pocketpalette.client.palette.PaletteEditModel;
import com.github.aruma256.pocketpalette.client.palette.PaletteEditModel.AddResult;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/**
 * Palette editor: the top half is the cycle being edited (reorder / delete),
 * the bottom half mirrors the player inventory — clicking an item appends its
 * base item to the cycle. Closing the screen commits and saves.
 */
@Environment(EnvType.CLIENT)
public class PaletteEditScreen extends Screen {

	private static final int PANEL_W = 176;
	private static final int PANEL_H = 222;
	private static final int ROW_H = 18;
	private static final int VISIBLE_ROWS = 4;
	private static final int SLOT_SIZE = 18;
	private static final int INV_COLUMNS = 9;
	private static final int INV_ROWS = 3; // main inventory rows (slots 9-35)

	private final PaletteEditModel model;

	private int group;
	private int scrollOffset = 0;
	private Component statusMessage = null;

	// layout, computed in init()
	private int left;
	private int top;
	private int listTop;
	private int invLeft;
	private int invTop;
	private int hotbarTop;

	public PaletteEditScreen(ItemStack seedStack) {
		super(Component.translatable("pocketpalette.editor.title"));
		this.model = new PaletteEditModel(ItemPalette.getGroupData(), seedStack);
		// Start on the first group the item is already registered in
		for (int g = 0; g < model.getGroupCount(); g++) {
			if (!model.isNewCycle(g)) {
				this.group = g;
				break;
			}
		}
	}

	@Override
	protected void init() {
		left = (this.width - PANEL_W) / 2;
		top = Math.max(8, (this.height - PANEL_H) / 2);
		listTop = top + 36;
		invLeft = left + (PANEL_W - INV_COLUMNS * SLOT_SIZE) / 2;
		invTop = top + 122;
		hotbarTop = invTop + INV_ROWS * SLOT_SIZE + 4;

		addRenderableWidget(Button.builder(
				Component.translatable("pocketpalette.editor.group", group + 1),
				button -> {
					group = (group + 1) % model.getGroupCount();
					scrollOffset = 0;
					statusMessage = null;
					rebuildWidgets();
				})
				.bounds(left + 6, top + 14, 70, 16)
				.build());

		List<ItemState> cycle = model.getCycle(group);
		scrollOffset = Math.max(0, Math.min(scrollOffset, cycle.size() - VISIBLE_ROWS));
		int visible = Math.min(VISIBLE_ROWS, cycle.size() - scrollOffset);
		for (int row = 0; row < visible; row++) {
			final int index = scrollOffset + row;
			int rowY = listTop + row * ROW_H;
			addRenderableWidget(Button.builder(Component.literal("↑"), button -> {
						if (model.moveUp(group, index)) rebuildWidgets();
					})
					.bounds(left + 122, rowY + 2, 14, 14)
					.build());
			addRenderableWidget(Button.builder(Component.literal("↓"), button -> {
						if (model.moveDown(group, index)) rebuildWidgets();
					})
					.bounds(left + 138, rowY + 2, 14, 14)
					.build());
			addRenderableWidget(Button.builder(Component.literal("✕"), button -> {
						if (model.remove(group, index)) rebuildWidgets();
					})
					.bounds(left + 154, rowY + 2, 14, 14)
					.build());
		}

		addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> onClose())
				.bounds(left + PANEL_W / 2 - 40, top + 202, 80, 16)
				.build());
	}

	@Override
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
		guiGraphics.fill(left - 2, top - 2, left + PANEL_W + 2, top + PANEL_H + 2, 0xF0101014);

		super.render(guiGraphics, mouseX, mouseY, partialTick);

		guiGraphics.drawCenteredString(this.font, this.title, left + PANEL_W / 2, top + 4, 0xFFFFFFFF);
		if (model.isNewCycle(group)) {
			guiGraphics.drawString(this.font,
					Component.translatable("pocketpalette.editor.new_cycle").withStyle(ChatFormatting.YELLOW),
					left + 82, top + 18, 0xFFFFFF55);
		}

		renderCycleList(guiGraphics, mouseX, mouseY);
		renderStatusLine(guiGraphics);
		renderInventoryGrid(guiGraphics, mouseX, mouseY);
	}

	private void renderCycleList(GuiGraphics guiGraphics, int mouseX, int mouseY) {
		List<ItemState> cycle = model.getCycle(group);
		int visible = Math.min(VISIBLE_ROWS, cycle.size() - scrollOffset);
		for (int row = 0; row < visible; row++) {
			int index = scrollOffset + row;
			int rowY = listTop + row * ROW_H;
			guiGraphics.fill(left + 6, rowY, left + PANEL_W - 6, rowY + ROW_H - 1, 0x30FFFFFF);

			ItemStack stack = cycle.get(index).toItemStack();
			guiGraphics.renderItem(stack, left + 8, rowY + 1);
			String name = this.font.plainSubstrByWidth(stack.getHoverName().getString(), 90);
			guiGraphics.drawString(this.font, name, left + 28, rowY + 5, 0xFFFFFFFF);

			if (mouseX >= left + 8 && mouseX < left + 24 && mouseY >= rowY + 1 && mouseY < rowY + 17) {
				guiGraphics.setTooltipForNextFrame(this.font, stack, mouseX, mouseY);
			}
		}
		if (scrollOffset > 0) {
			guiGraphics.drawString(this.font, "▲", left + PANEL_W - 16, listTop - 8, 0xFFAAAAAA);
		}
		if (scrollOffset + VISIBLE_ROWS < cycle.size()) {
			guiGraphics.drawString(this.font, "▼", left + PANEL_W - 16, listTop + VISIBLE_ROWS * ROW_H, 0xFFAAAAAA);
		}
	}

	private void renderStatusLine(GuiGraphics guiGraphics) {
		Component line = statusMessage;
		if (line == null) {
			line = Component.translatable("pocketpalette.editor.inventory_hint").withStyle(ChatFormatting.GRAY);
		}
		guiGraphics.drawCenteredString(this.font, line, left + PANEL_W / 2, top + 110, 0xFFFFFFFF);
	}

	private void renderInventoryGrid(GuiGraphics guiGraphics, int mouseX, int mouseY) {
		Inventory inventory = this.minecraft.player.getInventory();
		for (int i = 0; i < INV_COLUMNS * INV_ROWS + INV_COLUMNS; i++) {
			int slotIndex = toInventorySlot(i);
			int x = slotX(i);
			int y = slotY(i);
			guiGraphics.fill(x, y, x + SLOT_SIZE - 2, y + SLOT_SIZE - 2, 0x50FFFFFF);

			ItemStack stack = inventory.getItem(slotIndex);
			if (stack.isEmpty()) {
				continue;
			}
			guiGraphics.renderItem(stack, x, y);
			guiGraphics.renderItemDecorations(this.font, stack, x, y);
			if (mouseX >= x && mouseX < x + SLOT_SIZE - 2 && mouseY >= y && mouseY < y + SLOT_SIZE - 2) {
				guiGraphics.fill(x, y, x + SLOT_SIZE - 2, y + SLOT_SIZE - 2, 0x80FFFFFF);
				guiGraphics.setTooltipForNextFrame(this.font, stack, mouseX, mouseY);
			}
		}
	}

	/**
	 * Map a display cell (0-26: main inventory, 27-35: hotbar) to the
	 * player inventory slot index (hotbar is 0-8, main is 9-35).
	 */
	private static int toInventorySlot(int cell) {
		if (cell < INV_COLUMNS * INV_ROWS) {
			return cell + INV_COLUMNS;
		}
		return cell - INV_COLUMNS * INV_ROWS;
	}

	private int slotX(int cell) {
		return invLeft + (cell % INV_COLUMNS) * SLOT_SIZE + 1;
	}

	private int slotY(int cell) {
		if (cell < INV_COLUMNS * INV_ROWS) {
			return invTop + (cell / INV_COLUMNS) * SLOT_SIZE + 1;
		}
		return hotbarTop + 1;
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (super.mouseClicked(event, doubleClick)) {
			return true;
		}
		if (event.button() != 0) {
			return false;
		}
		double mouseX = event.x();
		double mouseY = event.y();
		for (int i = 0; i < INV_COLUMNS * INV_ROWS + INV_COLUMNS; i++) {
			int x = slotX(i);
			int y = slotY(i);
			if (mouseX >= x && mouseX < x + SLOT_SIZE - 2 && mouseY >= y && mouseY < y + SLOT_SIZE - 2) {
				addFromInventory(toInventorySlot(i));
				return true;
			}
		}
		return false;
	}

	private void addFromInventory(int slotIndex) {
		ItemStack stack = this.minecraft.player.getInventory().getItem(slotIndex);
		if (stack.isEmpty()) {
			return;
		}
		AddResult result = model.add(group, stack);
		switch (result) {
			case ADDED -> {
				statusMessage = null;
				scrollOffset = Math.max(0, model.getCycle(group).size() - VISIBLE_ROWS);
				this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
				rebuildWidgets();
			}
			case DUPLICATE_IN_CYCLE -> statusMessage =
					Component.translatable("pocketpalette.editor.duplicate_in_cycle").withStyle(ChatFormatting.RED);
			case DUPLICATE_IN_GROUP -> statusMessage =
					Component.translatable("pocketpalette.editor.duplicate_in_group").withStyle(ChatFormatting.RED);
			case INVALID -> { /* ignore */ }
		}
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		List<ItemState> cycle = model.getCycle(group);
		boolean overList = mouseX >= left && mouseX < left + PANEL_W
				&& mouseY >= listTop && mouseY < listTop + VISIBLE_ROWS * ROW_H;
		if (overList && cycle.size() > VISIBLE_ROWS && scrollY != 0) {
			int next = Math.max(0, Math.min(scrollOffset - (scrollY > 0 ? 1 : -1), cycle.size() - VISIBLE_ROWS));
			if (next != scrollOffset) {
				scrollOffset = next;
				rebuildWidgets();
			}
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		// Let the inventory key close the editor, like a container screen
		if (this.minecraft.options.keyInventory.matches(event)) {
			onClose();
			return true;
		}
		return super.keyPressed(event);
	}

	@Override
	public void onClose() {
		if (!PaletteConfigManager.isEditingAllowed()) {
			PocketPaletteClient.showConfigError("pocketpalette.config.editing_disabled");
			super.onClose();
			return;
		}
		boolean discarded = false;
		for (int g = 0; g < model.getGroupCount(); g++) {
			if (model.willDiscardCycle(g)) {
				discarded = true;
			}
		}
		if (!PaletteConfigManager.commitAndSave(model)) {
			PocketPaletteClient.showConfigError("pocketpalette.config.save_failed");
		}
		if (discarded) {
			this.minecraft.getChatListener().handleSystemMessage(
					Component.translatable("pocketpalette.editor.cycle_removed").withStyle(ChatFormatting.YELLOW),
					false);
		}
		super.onClose();
	}
}
