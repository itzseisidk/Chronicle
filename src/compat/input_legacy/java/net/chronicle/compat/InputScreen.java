package net.chronicle.compat;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

// on* returns true = event consumed, vanilla doesn't get it anymore
public abstract class InputScreen extends Screen {
	public static final int SHIFT = 1;
	public static final int CTRL = 2;
	public static final int ALT = 4;

	// no doubleClick flag before 1.21.9, so count it ourselves
	private static final long DOUBLE_CLICK_MS = 250;
	private long lastClickTime;
	private double lastClickX, lastClickY;

	protected InputScreen(Component title) {
		super(title);
	}

	protected boolean onMouseDown(double x, double y, int button, boolean doubleClick) {
		return false;
	}

	protected boolean onMouseUp(double x, double y, int button) {
		return false;
	}

	protected boolean onMouseDrag(double x, double y, int button, double dx, double dy) {
		return false;
	}

	protected boolean onScroll(double x, double y, double horizontal, double vertical) {
		return false;
	}

	protected boolean onKey(int key, int modifiers) {
		return false;
	}

	protected boolean onChar(int codepoint) {
		return false;
	}

	@Override
	public boolean mouseClicked(double x, double y, int button) {
		long now = System.currentTimeMillis();
		boolean doubleClick = now - lastClickTime < DOUBLE_CLICK_MS && Math.abs(x - lastClickX) < 2 && Math.abs(y - lastClickY) < 2;
		lastClickTime = now;
		lastClickX = x;
		lastClickY = y;
		return onMouseDown(x, y, button, doubleClick) || super.mouseClicked(x, y, button);
	}

	@Override
	public boolean mouseReleased(double x, double y, int button) {
		boolean handled = onMouseUp(x, y, button);
		return super.mouseReleased(x, y, button) || handled;
	}

	@Override
	public boolean mouseDragged(double x, double y, int button, double dx, double dy) {
		return onMouseDrag(x, y, button, dx, dy) || super.mouseDragged(x, y, button, dx, dy);
	}

	@Override
	public boolean mouseScrolled(double x, double y, double horizontal, double vertical) {
		return onScroll(x, y, horizontal, vertical) || super.mouseScrolled(x, y, horizontal, vertical);
	}

	@Override
	public boolean keyPressed(int key, int scancode, int modifiers) {
		int mods = (modifiers & SHIFT) | (hasControlDown() ? CTRL : 0) | (modifiers & ALT);
		return onKey(key, mods) || super.keyPressed(key, scancode, modifiers);
	}

	@Override
	public boolean charTyped(char c, int modifiers) {
		return onChar(c) || super.charTyped(c, modifiers);
	}
}
