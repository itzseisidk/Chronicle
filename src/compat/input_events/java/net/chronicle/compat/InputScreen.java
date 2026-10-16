package net.chronicle.compat;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

// on* returns true = event consumed, vanilla doesn't get it anymore
public abstract class InputScreen extends Screen {
	public static final int SHIFT = 1;
	public static final int CTRL = 2;
	public static final int ALT = 4;

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
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		return onMouseDown(event.x(), event.y(), event.button(), doubleClick) || super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		boolean handled = onMouseUp(event.x(), event.y(), event.button());
		return super.mouseReleased(event) || handled;
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
		return onMouseDrag(event.x(), event.y(), event.button(), dx, dy) || super.mouseDragged(event, dx, dy);
	}

	@Override
	public boolean mouseScrolled(double x, double y, double horizontal, double vertical) {
		return onScroll(x, y, horizontal, vertical) || super.mouseScrolled(x, y, horizontal, vertical);
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		int mods = (event.hasShiftDown() ? SHIFT : 0) | (event.hasControlDown() ? CTRL : 0) | (event.hasAltDown() ? ALT : 0);
		return onKey(event.key(), mods) || super.keyPressed(event);
	}

	@Override
	public boolean charTyped(CharacterEvent event) {
		return onChar(event.codepoint()) || super.charTyped(event);
	}
}
