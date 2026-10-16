package net.chronicle.ui.modern.widget;

import java.util.ArrayList;
import java.util.List;
import net.chronicle.compat.Gfx;
import net.chronicle.ui.modern.Anim;
import net.chronicle.ui.modern.Draw;
import net.chronicle.ui.modern.Theme;

/** Mehrzeiliges Textfeld mit Zeilenumbruch, Scrollen und Markierung. */
public class MTextArea extends MTextBase {
	private static final int PAD = 6;
	private static final int LINE = 11;

	/** Zeilen als {start, end} im Gesamttext (end exklusiv, ohne Zeilenumbruch). */
	private final List<int[]> lines = new ArrayList<>();
	private int wrappedFor = -1;
	private boolean dirty = true;
	private final Anim scroll = new Anim(0, 20);
	private float scrollTarget;

	public MTextArea() {
		maxLength = 1000;
	}

	@Override
	protected boolean allowNewlines() {
		return true;
	}

	@Override
	protected void onTextChanged() {
		dirty = true;
	}

	private int innerWidth() {
		return w - PAD * 2 - 4;
	}

	private void wrap() {
		if (!dirty && wrappedFor == innerWidth()) return;
		dirty = false;
		wrappedFor = innerWidth();
		lines.clear();
		int maxW = Math.max(10, innerWidth());
		int paragraphStart = 0;
		while (true) {
			int nl = value.indexOf('\n', paragraphStart);
			int paragraphEnd = nl < 0 ? value.length() : nl;
			int start = paragraphStart;
			if (start == paragraphEnd) lines.add(new int[] {start, start});
			while (start < paragraphEnd) {
				String rest = value.substring(start, paragraphEnd);
				int fit = font().plainSubstrByWidth(rest, maxW).length();
				if (fit <= 0) fit = 1;
				int end = start + fit;
				if (end < paragraphEnd) {
					int space = value.lastIndexOf(' ', end - 1);
					if (space >= start + 1) end = space + 1;
				}
				lines.add(new int[] {start, end});
				start = end;
			}
			if (nl < 0) break;
			paragraphStart = nl + 1;
		}
	}

	private int lineOf(int pos) {
		for (int i = 0; i < lines.size(); i++) {
			int[] l = lines.get(i);
			boolean last = i == lines.size() - 1;
			boolean softWrapEnd = !last && lines.get(i + 1)[0] == l[1];
			if (pos >= l[0] && (pos < l[1] || (pos == l[1] && !softWrapEnd))) return i;
		}
		return Math.max(0, lines.size() - 1);
	}

	private int xOf(int pos) {
		int[] l = lines.get(lineOf(pos));
		return font().width(value.substring(l[0], Math.min(pos, l[1])));
	}

	private int indexInLine(int line, int px) {
		int[] l = lines.get(Math.max(0, Math.min(lines.size() - 1, line)));
		String text = value.substring(l[0], l[1]);
		if (text.endsWith(" ") && line < lines.size() - 1 && lines.get(line + 1)[0] == l[1]) {
			text = text.substring(0, text.length() - 1);
		}
		int idx = font().plainSubstrByWidth(text, Math.max(0, px)).length();
		if (idx < text.length()) {
			int before = font().width(text.substring(0, idx));
			int after = font().width(text.substring(0, idx + 1));
			if (px - before > after - px) idx++;
		}
		return l[0] + idx;
	}

	@Override
	protected int lineStart(int pos) {
		wrap();
		return lines.get(lineOf(pos))[0];
	}

	@Override
	protected int lineEnd(int pos) {
		wrap();
		int i = lineOf(pos);
		int[] l = lines.get(i);
		boolean softWrap = i < lines.size() - 1 && lines.get(i + 1)[0] == l[1];
		return softWrap ? l[1] - 1 : l[1];
	}

	@Override
	protected boolean vertical(int direction, boolean select) {
		wrap();
		int line = lineOf(cursor) + direction;
		if (line < 0) {
			moveTo(0, select);
		} else if (line >= lines.size()) {
			moveTo(value.length(), select);
		} else {
			moveTo(indexInLine(line, xOf(cursor)), select);
		}
		return true;
	}

	private int contentHeight() {
		return lines.size() * LINE + PAD * 2;
	}

	private void clampScroll() {
		float max = Math.max(0, contentHeight() - h);
		scrollTarget = Math.max(0, Math.min(max, scrollTarget));
		scroll.set(scrollTarget);
	}

	private void keepCursorVisible() {
		int cy = lineOf(cursor) * LINE;
		if (cy < scrollTarget) scrollTarget = cy;
		if (cy + LINE + PAD * 2 > scrollTarget + h) scrollTarget = cy + LINE + PAD * 2 - h;
		clampScroll();
	}

	private int lastCursor = -1;

	@Override
	protected void draw(Gfx g, int mx, int my, float dt) {
		wrap();
		if (cursor != lastCursor) {
			keepCursorVisible();
			lastCursor = cursor;
		}
		float sc = scroll.update(dt);
		int border = focused ? Theme.ACCENT : Draw.lerp(Theme.PANEL_BORDER, 0xFF39425A, hover.get());
		Draw.panel(g, x, y, w, h, 5, Theme.SURFACE, border);

		int left = x + PAD;
		int top = y + PAD - Math.round(sc);
		if (value.isEmpty() && !focused) {
			g.text(font(), placeholder.getString(), left, y + PAD + 1, Theme.TEXT_MUTED, false);
		}
		g.scissor(x + 1, y + 1, x + w - 1, y + h - 1);
		int cursorLine = lineOf(cursor);
		for (int i = 0; i < lines.size(); i++) {
			int ly = top + i * LINE;
			if (ly + LINE < y || ly > y + h) continue;
			int[] l = lines.get(i);
			if (hasSelection()) {
				int s = Math.max(selStart(), l[0]), e = Math.min(selEnd(), l[1]);
				if (e > s || (selStart() <= l[0] && selEnd() > l[1])) {
					int sx = left + font().width(value.substring(l[0], s));
					int ex = left + font().width(value.substring(l[0], Math.max(s, e)));
					if (selEnd() > l[1]) ex += 3;
					g.fill(sx, ly, ex, ly + 10, 0x808B5CF6);
				}
			}
			g.text(font(), value.substring(l[0], l[1]), left, ly + 1, Theme.TEXT, false);
			if (i == cursorLine && cursorVisible()) {
				int cx = left + xOf(cursor);
				g.fill(cx, ly, cx + 1, ly + 10, Theme.ACCENT_HOVER);
			}
		}
		g.endScissor();

		if (contentHeight() > h) {
			float ratio = (float) h / contentHeight();
			int barH = Math.max(12, Math.round((h - 4) * ratio));
			float max = contentHeight() - h;
			int barY = y + 2 + Math.round((h - 4 - barH) * (sc / max));
			Draw.round(g, x + w - 5, barY, 3, barH, 1.5f, 0x60FFFFFF);
		}

		String counter = value.length() + "/" + maxLength;
		if (focused) {
			g.text(font(), counter, x + w - 6 - font().width(counter), y + h + 3, Theme.TEXT_MUTED, false);
		}
	}

	private int indexAt(double mx, double my) {
		int line = (int) Math.floor((my - (y + PAD) + scroll.get()) / LINE);
		line = Math.max(0, Math.min(lines.size() - 1, line));
		return indexInLine(line, (int) mx - (x + PAD));
	}

	@Override
	public boolean mouseDown(double mx, double my, int button) {
		if (!contains(mx, my)) return false;
		wrap();
		setFocused(true);
		moveTo(indexAt(mx, my), false);
		selecting = true;
		return true;
	}

	@Override
	public boolean mouseDrag(double mx, double my, int button, double dx, double dy) {
		if (!selecting) return false;
		moveTo(indexAt(mx, my), true);
		return true;
	}

	@Override
	public boolean scroll(double mx, double my, double amount) {
		if (!contains(mx, my)) return false;
		scrollTarget -= (float) amount * LINE * 2;
		clampScroll();
		return true;
	}
}
