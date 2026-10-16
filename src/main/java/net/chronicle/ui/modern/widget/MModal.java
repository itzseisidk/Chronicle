package net.chronicle.ui.modern.widget;

import com.mojang.blaze3d.platform.InputConstants;
import net.chronicle.compat.InputScreen;
import java.util.ArrayList;
import java.util.List;
import net.chronicle.compat.Gfx;
import net.chronicle.ui.modern.Anim;
import net.chronicle.ui.modern.Draw;
import net.chronicle.ui.modern.Theme;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

public abstract class MModal {
	protected final Anim open = new Anim(0, 14);
	protected final List<MWidget> widgets = new ArrayList<>();
	protected final Component title;
	protected int x, y, w, h;
	private final int prefW, prefH;
	private boolean closing;
	private int offsetY;

	protected MModal(Component title, int prefW, int prefH) {
		this.title = title;
		this.prefW = prefW;
		this.prefH = prefH;
	}

	public void layout(int screenW, int screenH) {
		w = Math.min(prefW, screenW - 24);
		h = Math.min(prefH, screenH - 24);
		x = (screenW - w) / 2;
		y = (screenH - h) / 2;
		widgets.clear();
		build();
	}

	// x/y/w/h are already set here
	protected abstract void build();

	// enter key
	protected void submit() {
	}

	protected void drawContent(Gfx g, int mx, int my, float dt) {
	}

	protected <T extends MWidget> T add(T widget) {
		widgets.add(widget);
		return widget;
	}

	public void close() {
		closing = true;
		open.set(0);
	}

	public boolean isClosed() {
		return closing && open.get() < 0.02f;
	}

	public void render(Gfx g, int mx, int my, float dt, int screenW, int screenH) {
		if (!closing) open.set(1);
		float o = open.update(dt);
		g.fill(0, 0, screenW, screenH, Draw.alpha(0xA0000000, o));
		offsetY = Math.round((1 - o) * 10);
		my -= offsetY;
		g.push();
		g.translate(0, offsetY);
		Draw.shadow(g, x, y, w, h, 8, 8, o);
		Draw.panel(g, x, y, w, h, 8, Theme.PANEL, Theme.PANEL_BORDER);
		g.text(MWidget.font(), title.copy().withStyle(ChatFormatting.BOLD), x + 12, y + 11, Theme.TEXT, false);
		boolean overClose = mx >= x + w - 22 && mx < x + w - 6 && my >= y + 6 && my < y + 22;
		Draw.round(g, x + w - 22, y + 6, 16, 16, 4, overClose ? Theme.SURFACE_HOVER : 0);
		g.text(MWidget.font(), "✕", x + w - 17, y + 10, overClose ? Theme.TEXT : Theme.TEXT_MUTED, false);
		drawContent(g, mx, my, dt);
		for (MWidget widget : widgets) widget.render(g, mx, my, dt);
		g.pop();
	}

	private MWidget focused() {
		for (MWidget widget : widgets) {
			if (widget.isFocused()) return widget;
		}
		return null;
	}

	protected void focus(MWidget target) {
		for (MWidget widget : widgets) widget.setFocused(widget == target);
	}

	public boolean mouseDown(double mx, double my, int button) {
		my -= offsetY;
		// clicked outside -> close
		if (mx < x || mx >= x + w || my < y || my >= y + h) {
			close();
			return true;
		}
		if (mx >= x + w - 22 && mx < x + w - 6 && my >= y + 6 && my < y + 22) {
			MWidget.click();
			close();
			return true;
		}
		for (MWidget widget : widgets) {
			if (widget.contains(mx, my)) {
				focus(widget.focusable() ? widget : null);
				widget.mouseDown(mx, my, button);
				return true;
			}
		}
		focus(null);
		return true;
	}

	public boolean mouseUp(double mx, double my, int button) {
		my -= offsetY;
		for (MWidget widget : widgets) widget.mouseUp(mx, my, button);
		return true;
	}

	public boolean mouseDrag(double mx, double my, int button, double dx, double dy) {
		my -= offsetY;
		for (MWidget widget : widgets) {
			if (widget.mouseDrag(mx, my, button, dx, dy)) return true;
		}
		return true;
	}

	public boolean scroll(double mx, double my, double amount) {
		my -= offsetY;
		for (MWidget widget : widgets) {
			if (widget.scroll(mx, my, amount)) return true;
		}
		return true;
	}

	public boolean key(int key, int modifiers) {
		MWidget focused = focused();
		if (key == InputConstants.KEY_ESCAPE) {
			close();
			return true;
		}
		if (key == InputConstants.KEY_TAB) {
			List<MWidget> focusable = widgets.stream().filter(MWidget::focusable).toList();
			if (!focusable.isEmpty()) {
				int i = focusable.indexOf(focused);
				boolean back = (modifiers & InputScreen.SHIFT) != 0;
				focus(focusable.get(Math.floorMod(i + (back ? -1 : 1), focusable.size())));
			}
			return true;
		}
		if (focused != null && focused.key(key, modifiers)) return true;
		if (key == InputConstants.KEY_RETURN || key == InputConstants.KEY_NUMPADENTER) {
			submit();
			return true;
		}
		return true;
	}

	public boolean charTyped(int codepoint) {
		MWidget focused = focused();
		if (focused != null) focused.charTyped(codepoint);
		return true;
	}
}
