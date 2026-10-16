package net.chronicle.ui.modern.widget;

import net.chronicle.compat.Gfx;
import net.chronicle.ui.modern.Draw;
import net.chronicle.ui.modern.Theme;

public class MTextField extends MTextBase {
	private final String icon;
	private int scroll;

	public MTextField(String icon) {
		this.icon = icon;
	}

	private int textLeft() {
		return x + (icon == null ? 7 : 19);
	}

	private int textWidth() {
		return w - (textLeft() - x) - (value.isEmpty() ? 6 : 18);
	}

	@Override
	protected void onTextChanged() {
		scroll = Math.min(scroll, value.length());
	}

	private void keepCursorVisible() {
		if (cursor < scroll) scroll = cursor;
		while (scroll < cursor && font().width(value.substring(scroll, cursor)) > textWidth()) scroll++;
	}

	@Override
	protected void draw(Gfx g, int mx, int my, float dt) {
		int border = focused ? Theme.ACCENT : Draw.lerp(Theme.PANEL_BORDER, 0xFF39425A, hover.get());
		Draw.panel(g, x, y, w, h, 5, Theme.SURFACE, border);
		int ty = y + (h - 8) / 2;
		if (icon != null) {
			g.text(font(), icon, x + 7, ty, focused ? Theme.ACCENT_HOVER : Theme.TEXT_MUTED, false);
		}
		int left = textLeft();
		if (value.isEmpty() && !focused) {
			g.text(font(), Draw.ellipsize(font(), placeholder.getString(), textWidth()), left, ty, Theme.TEXT_MUTED, false);
		}

		keepCursorVisible();
		String visible = font().plainSubstrByWidth(value.substring(scroll), textWidth());
		int visEnd = scroll + visible.length();
		g.scissor(left - 1, y + 1, left + textWidth() + 1, y + h - 1);
		if (hasSelection()) {
			int s = Math.max(selStart(), scroll), e = Math.min(selEnd(), visEnd);
			if (e > s) {
				int sx = left + font().width(value.substring(scroll, s));
				int ex = left + font().width(value.substring(scroll, e));
				g.fill(sx, ty - 1, ex, ty + 9, 0x808B5CF6);
			}
		}
		g.text(font(), visible, left, ty, Theme.TEXT, false);
		if (cursorVisible()) {
			int cx = left + font().width(value.substring(scroll, Math.min(cursor, visEnd)));
			g.fill(cx, ty - 1, cx + 1, ty + 9, Theme.ACCENT_HOVER);
		}
		g.endScissor();

		if (!value.isEmpty()) {
			boolean overClear = mx >= x + w - 16 && mx < x + w && my >= y && my < y + h;
			g.text(font(), "✕", x + w - 12, ty, overClear ? Theme.TEXT : Theme.TEXT_MUTED, false);
		}
	}

	private int indexAt(double mx) {
		int rel = (int) mx - textLeft();
		String rest = value.substring(scroll);
		int idx = font().plainSubstrByWidth(rest, Math.max(0, rel)).length();
		if (idx < rest.length()) {
			int before = font().width(rest.substring(0, idx));
			int after = font().width(rest.substring(0, idx + 1));
			if (rel - before > after - rel) idx++;
		}
		return scroll + idx;
	}

	@Override
	public boolean mouseDown(double mx, double my, int button) {
		if (!contains(mx, my)) return false;
		if (!value.isEmpty() && mx >= x + w - 16) {
			setValue("");
			setFocused(true);
			click();
			return true;
		}
		setFocused(true);
		moveTo(indexAt(mx), false);
		selecting = true;
		return true;
	}

	@Override
	public boolean mouseDrag(double mx, double my, int button, double dx, double dy) {
		if (!selecting) return false;
		moveTo(indexAt(mx), true);
		return true;
	}
}
