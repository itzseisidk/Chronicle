package net.chronicle.ui.modern.widget;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.chronicle.compat.Gfx;
import net.chronicle.journal.Category;
import net.chronicle.journal.EntryText;
import net.chronicle.journal.ItemIcons;
import net.chronicle.journal.JournalEntry;
import net.chronicle.journal.JournalManager;
import net.chronicle.ui.modern.Anim;
import net.chronicle.ui.modern.Draw;
import net.chronicle.ui.modern.Theme;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

/** Scrollbare Karten-Liste. Ein Klick klappt eine Karte auf und zeigt Details und Aktionen. */
public class MCardList extends MWidget {
	public interface Actions {
		void target(JournalEntry entry);

		void pin(JournalEntry entry);

		void copy(JournalEntry entry);

		void edit(JournalEntry entry);

		void delete(JournalEntry entry);
	}

	private static final int COLLAPSED = 32;
	private static final int GAP = 4;
	private static final int BUTTON_H = 18;

	private final Actions actions;
	private final List<Card> cards = new ArrayList<>();
	private final Anim scroll = new Anim(0, 18);
	private float scrollTarget;
	private int contentHeight;
	private boolean draggingBar;
	private String expandedId;
	private String copiedId;
	private long copiedAt;
	public Component emptyTitle = Component.empty();
	public Component emptyHint = Component.empty();

	public MCardList(Actions actions) {
		this.actions = actions;
	}

	private final class Card {
		final JournalEntry entry;
		final Anim expand = new Anim(0, 16);
		final Anim hover = new Anim(0, 18);
		final List<MButton> buttons = new ArrayList<>();
		List<FormattedCharSequence> detail = List.of();
		int top;
		int height = COLLAPSED;
		boolean iconOnly;

		Card(JournalEntry entry) {
			this.entry = entry;
			buttons.add(new MButton(null, "◎", MButton.Style.SECONDARY, () -> actions.target(entry)));
			buttons.add(new MButton(null, "★", MButton.Style.SECONDARY, () -> actions.pin(entry)));
			buttons.add(new MButton(null, "⧉", MButton.Style.SECONDARY, () -> {
				copiedId = entry.id;
				copiedAt = System.currentTimeMillis();
				actions.copy(entry);
			}));
			if (entry.category == Category.NOTE) {
				buttons.add(new MButton(Component.translatable("chronicle.button.edit"), "✎", MButton.Style.SECONDARY, () -> actions.edit(entry)));
			}
			buttons.add(new MButton(Component.translatable("chronicle.button.delete"), "✖", MButton.Style.DANGER, () -> actions.delete(entry)));
			buttons.get(1).iconColor = Theme.GOLD;
			buttons.get(0).iconColor = Theme.CYAN;
		}

		void refreshLabels() {
			boolean target = JournalManager.isTarget(entry);
			buttons.get(0).label = Component.translatable(target ? "chronicle.button.untarget" : "chronicle.button.target");
			buttons.get(1).label = Component.translatable(entry.pinned ? "chronicle.button.unpin" : "chronicle.button.pin");
			boolean copied = entry.id.equals(copiedId) && System.currentTimeMillis() - copiedAt < 1500;
			buttons.get(2).label = Component.translatable(copied ? "chronicle.button.copied" : "chronicle.button.copy");
			buttons.get(2).icon = copied ? "✓" : "⧉";
			buttons.get(2).iconColor = copied ? Theme.SUCCESS : 0;
		}

		int extraHeight() {
			return 6 + detail.size() * 10 + 6 + BUTTON_H + 8;
		}
	}

	public void setEntries(List<JournalEntry> entries) {
		Map<String, Card> old = new HashMap<>();
		for (Card c : cards) old.put(c.entry.id, c);
		cards.clear();
		for (JournalEntry e : entries) {
			Card c = old.get(e.id);
			cards.add(c != null && c.entry == e ? c : new Card(e));
		}
	}

	public boolean isEmpty() {
		return cards.isEmpty();
	}

	public void scrollToTop() {
		scrollTarget = 0;
	}

	private boolean scrollbarVisible() {
		return contentHeight > h;
	}

	private int cardWidth() {
		return w - (scrollbarVisible() ? 8 : 0);
	}

	private void layout(float dt) {
		int textWidth = cardWidth() - 46;
		int acc = 0;
		for (Card c : cards) {
			c.expand.set(c.entry.id.equals(expandedId) ? 1 : 0);
			float ex = c.expand.update(dt);
			if (ex > 0.001f) {
				c.detail = font().split(EntryText.details(c.entry), Math.max(40, textWidth));
			}
			c.top = acc;
			c.height = COLLAPSED + Math.round(c.extraHeight() * ex);
			acc += c.height + GAP;
		}
		contentHeight = Math.max(0, acc - GAP);
		float max = Math.max(0, contentHeight - h);
		scrollTarget = Math.max(0, Math.min(max, scrollTarget));
		scroll.set(scrollTarget);
	}

	@Override
	protected void draw(Gfx g, int mx, int my, float dt) {
		layout(dt);
		float sc = scroll.update(dt);
		boolean inside = contains(mx, my);

		if (cards.isEmpty()) {
			int cy = y + h / 2 - 14;
			Draw.circle(g, x + w / 2f, cy - 10, 12, Theme.ACCENT_SOFT);
			g.item(ItemIcons.stack("minecraft:writable_book"), x + w / 2 - 8, cy - 18);
			int tw = font().width(emptyTitle);
			g.text(font(), emptyTitle, x + (w - tw) / 2, cy + 8, Theme.TEXT, false);
			List<FormattedCharSequence> hint = font().split(emptyHint, Math.max(60, w - 40));
			for (int i = 0; i < hint.size(); i++) {
				int lw = font().width(hint.get(i));
				g.text(font(), hint.get(i), x + (w - lw) / 2, cy + 22 + i * 10, Theme.TEXT_MUTED, false);
			}
			return;
		}

		g.scissor(x, y, x + w, y + h);
		for (Card c : cards) {
			int cy = y + c.top - Math.round(sc);
			if (cy > y + h || cy + c.height < y) continue;
			boolean over = inside && mx >= x && mx < x + cardWidth() && my >= cy && my < cy + c.height;
			c.hover.set(over ? 1 : 0);
			c.hover.update(dt);
			drawCard(g, c, cy, mx, my, dt);
		}
		g.endScissor();

		if (scrollbarVisible()) {
			float ratio = (float) h / contentHeight;
			int barH = Math.max(16, Math.round(h * ratio));
			float max = contentHeight - h;
			int barY = y + Math.round((h - barH) * (sc / max));
			boolean overBar = mx >= x + w - 6 && mx < x + w && my >= y && my < y + h;
			Draw.round(g, x + w - 4, y, 3, h, 1.5f, 0x14FFFFFF);
			Draw.round(g, x + w - 4, barY, 3, barH, 1.5f, draggingBar || overBar ? Theme.ACCENT_HOVER : 0x50FFFFFF);
		}
	}

	private void drawCard(Gfx g, Card c, int cy, int mx, int my, float dt) {
		JournalEntry e = c.entry;
		int cw = cardWidth();
		float ex = c.expand.get();
		int bg = Draw.lerp(Theme.SURFACE, Theme.SURFACE_HOVER, Math.max(c.hover.get(), ex));
		Draw.round(g, x, cy, cw, c.height, 6, bg);
		Draw.round(g, x, cy + 7, 2.5f, COLLAPSED - 14, 1.25f, e.category.color);

		Draw.circle(g, x + 21, cy + 16, 11, Draw.alpha(e.category.color, 0.16f));
		g.item(ItemIcons.icon(e), x + 13, cy + 8);

		boolean target = JournalManager.isTarget(e);
		int right = x + cw - 8;
		String chevron = ex > 0.5f ? "▴" : "▾";
		g.text(font(), chevron, right - font().width(chevron), cy + 12, Theme.TEXT_MUTED, false);
		right -= font().width(chevron) + 6;
		if (e.pinned) {
			right -= font().width("★");
			g.text(font(), "★", right, cy + 12, Theme.GOLD, false);
			right -= 5;
		}
		if (target) {
			right -= font().width("◎");
			g.text(font(), "◎", right, cy + 12, Theme.CYAN, false);
			right -= 5;
		}
		int textMax = right - (x + 38) - 2;
		g.text(font(), Draw.ellipsize(font(), e.title(), textMax), x + 38, cy + 7, Theme.TEXT, false);
		g.text(font(), Draw.ellipsize(font(), EntryText.subtitle(e), textMax), x + 38, cy + 18, Theme.TEXT_MUTED, false);

		if (ex <= 0.01f) return;
		g.scissor(x, cy + COLLAPSED - 1, x + cw, cy + c.height);
		g.fill(x + 38, cy + COLLAPSED, x + cw - 8, cy + COLLAPSED + 1, Theme.DIVIDER);
		int ly = cy + COLLAPSED + 6;
		for (FormattedCharSequence line : c.detail) {
			g.text(font(), line, x + 38, ly, Theme.TEXT_DIM, false);
			ly += 10;
		}
		c.refreshLabels();
		layoutButtons(c, x + 38, ly + 6, cw - 46);
		for (MButton b : c.buttons) {
			b.render(g, mx, my, dt);
		}
		g.endScissor();
	}

	private void layoutButtons(Card c, int bx, int by, int avail) {
		int total = 0;
		for (MButton b : c.buttons) total += buttonWidth(b, false) + 4;
		c.iconOnly = total - 4 > avail;
		for (MButton b : c.buttons) {
			int bw = buttonWidth(b, c.iconOnly);
			b.tooltip = c.iconOnly ? b.label : null;
			b.bounds(bx, by, bw, BUTTON_H);
			bx += bw + 4;
		}
	}

	private int buttonWidth(MButton b, boolean iconOnly) {
		if (iconOnly || b.label == null) return 20;
		return font().width(b.icon) + 4 + font().width(b.label) + 14;
	}

	/** Liefert den Button unter der Maus (für Tooltips im Kompaktmodus). */
	public MButton hoveredButton(double mx, double my) {
		if (!contains(mx, my)) return null;
		for (Card c : cards) {
			if (c.expand.get() < 0.5f || !c.iconOnly) continue;
			for (MButton b : c.buttons) {
				if (b.contains(mx, my)) return b;
			}
		}
		return null;
	}

	private void scrollToBar(double my) {
		float max = Math.max(0, contentHeight - h);
		float ratio = (float) ((my - y) / h);
		scrollTarget = Math.max(0, Math.min(max, ratio * contentHeight - h / 2f));
	}

	@Override
	public boolean mouseDown(double mx, double my, int button) {
		if (!contains(mx, my)) return false;
		if (scrollbarVisible() && mx >= x + w - 6) {
			draggingBar = true;
			scrollToBar(my);
			return true;
		}
		int sc = Math.round(scroll.get());
		for (Card c : cards) {
			int cy = y + c.top - sc;
			if (my < cy || my >= cy + c.height || mx >= x + cardWidth()) continue;
			if (c.expand.get() > 0.5f) {
				for (MButton b : c.buttons) {
					if (b.mouseDown(mx, my, button)) return true;
				}
			}
			if (button == 0 && my < cy + COLLAPSED) {
				expandedId = c.entry.id.equals(expandedId) ? null : c.entry.id;
				click();
			}
			return true;
		}
		return true;
	}

	@Override
	public boolean mouseDrag(double mx, double my, int button, double dx, double dy) {
		if (!draggingBar) return false;
		scrollToBar(my);
		return true;
	}

	@Override
	public boolean mouseUp(double mx, double my, int button) {
		boolean was = draggingBar;
		draggingBar = false;
		return was;
	}

	@Override
	public boolean scroll(double mx, double my, double amount) {
		if (!contains(mx, my)) return false;
		scrollTarget -= (float) amount * 30;
		return true;
	}
}
