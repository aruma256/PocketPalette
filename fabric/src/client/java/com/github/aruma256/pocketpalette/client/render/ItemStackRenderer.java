package com.github.aruma256.pocketpalette.client.render;

import java.util.Collection;

import com.github.aruma256.pocketpalette.PocketPalette;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.ItemStack;

public final class ItemStackRenderer {

	// Animation timing constants
	private static final double CIRCLE_EXPAND_SPEED = 3.0;
	private static final double AFTERIMAGE_FADE_SPEED = 1.5;

	// Circle layout constants
	private static final double CIRCLE_BASE_RADIUS = 20.0;
	private static final double CIRCLE_RADIUS_PER_ITEM = 1.2;

	public static void renderItemStacks(GuiGraphicsExtractor guiGraphics, Collection<ItemStack> stacks, int x, int y, int t, float pt, int lt, byte direction) {
		if (stacks.isEmpty()) {
			return;
		}
		if (PocketPalette.CONFIG.DISABLE_ANIMATIONS) {
			t = Integer.MAX_VALUE;
			pt = 0;
		}
		double max_r = CIRCLE_BASE_RADIUS + stacks.size() * CIRCLE_RADIUS_PER_ITEM;
		double r = max_r * Math.tanh((t + pt) / CIRCLE_EXPAND_SPEED);
		double afterimage = 1 - Math.tanh((t + pt - lt) / AFTERIMAGE_FADE_SPEED);
		//
		int i = 0;
		for (ItemStack c: stacks) {
			// Screen y grows downward, so a positive angle step lays items out
			// clockwise starting from the bottom of the circle (i = 0)
			double theta = ((double)i - afterimage*direction) / stacks.size() * 2 * Math.PI + Math.PI / 2;
			double dx = r * Math.cos(theta);
			double dy = r * Math.sin(theta);
			renderAndDecorateItem(guiGraphics, c, (int)Math.round(x + dx), (int)Math.round(y + dy));
			i++;
		}
	}

	private static void renderAndDecorateItem(GuiGraphicsExtractor guiGraphics, ItemStack itemStack, int x, int y) {
		guiGraphics.item(itemStack, x, y);
		// Candidates are real inventory stacks; the count (and durability bar)
		// is what tells identical-item stacks apart on the ring
		guiGraphics.itemDecorations(Minecraft.getInstance().font, itemStack, x, y);
	}

}
