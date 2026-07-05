package com.github.aruma256.pocketpalette.client.event;

import java.util.ArrayList;

public class ScrollEvent {

	private static final ArrayList<ScrollListener> LISTENERS = new ArrayList<>();

	public static void registerListener(ScrollListener listener) {
		LISTENERS.add(listener);
	}

	public static boolean post(ScrollEvent event) {
		for (ScrollListener listener : LISTENERS) {
			listener.onScroll(event);
			if (event.isCanceled()) {
				return true;
			}
		}
		return false;
	}

	private boolean canceled = false;
	private final double scrollAmount;

	public ScrollEvent(double scrollAmount) {
		this.scrollAmount = scrollAmount;
	}

	public boolean isCanceled() {
		return canceled;
	}

	public void setCanceled(boolean value) {
		canceled = value;
	}

	public double getScrollDelta() {
		return scrollAmount;
	}

	public interface ScrollListener {
		void onScroll(ScrollEvent event);
	}

}
