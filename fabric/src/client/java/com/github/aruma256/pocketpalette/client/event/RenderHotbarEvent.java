package com.github.aruma256.pocketpalette.client.event;

import java.util.ArrayList;

import com.mojang.blaze3d.platform.Window;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

public class RenderHotbarEvent {
	private static final ArrayList<RenderHotbarListener> listeners = new ArrayList<>();

	public static void registerListener(RenderHotbarListener listener) {
		listeners.add(listener);
	}

	public static void post(GuiGraphics guiGraphics, float tickDelta) {
		RenderHotbarEvent event = new RenderHotbarEvent(guiGraphics, tickDelta);
		for (RenderHotbarListener iListener : listeners) {
			iListener.onRenderHotbar(event);
		}
	}

	private final GuiGraphics guiGraphics;
	private final float tickDelta;

	private RenderHotbarEvent(GuiGraphics guiGraphics, float tickDelta) {
		this.guiGraphics = guiGraphics;
		this.tickDelta = tickDelta;
	}

	public GuiGraphics getGuiGraphics() {
		return this.guiGraphics;
	}

	public float getPartialTicks() {
		return this.tickDelta;
	}

	public Window getWindow() {
		return Minecraft.getInstance().getWindow();
	}

	public static interface RenderHotbarListener {
		public void onRenderHotbar(RenderHotbarEvent event);
	}

}
