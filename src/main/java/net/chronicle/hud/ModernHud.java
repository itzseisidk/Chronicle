package net.chronicle.hud;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.chronicle.compat.Gfx;
import net.chronicle.journal.EntryText;
import net.chronicle.journal.ItemIcons;
import net.chronicle.journal.JournalEntry;
import net.chronicle.journal.Nav;
import net.chronicle.ui.modern.Anim;
import net.chronicle.ui.modern.Draw;
import net.chronicle.ui.modern.Theme;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/** HUD im Modern-Stil: einfahrende Benachrichtigungskarten und eine Kompass-Pille. */
public final class ModernHud {
	private static final long LIFETIME_MS = 4500;
	private static final int CARD_W = 176;
	private static final int CARD_H = 34;
	private static final List<Toast> TOASTS = new ArrayList<>();
	private static final Anim.Clock CLOCK = new Anim.Clock();
	private static final Anim COMPASS = new Anim(0, 10);
	private static float frameDelta;

	private ModernHud() {
	}

	private static final class Toast {
		final Component heading;
		final Component text;
		final JournalEntry entry;
		final long born = System.currentTimeMillis();
		final Anim slide = new Anim(0, 12);
		final Anim slot = new Anim(-1, 14);

		Toast(Component heading, Component text, JournalEntry entry) {
			this.heading = heading;
			this.text = text;
			this.entry = entry;
		}

		boolean expired() {
			return System.currentTimeMillis() - born > LIFETIME_MS;
		}
	}

	public static void push(Component heading, Component text, JournalEntry entry) {
		TOASTS.add(0, new Toast(heading, text, entry));
		while (TOASTS.size() > 4) TOASTS.remove(TOASTS.size() - 1);
	}

	public static void tick() {
	}

	static void hideCompass() {
		COMPASS.snap(0);
	}

	/** Beide Teile teilen sich eine Uhr – der erste Aufruf pro Frame misst. */
	private static float delta() {
		return frameDelta;
	}

	public static void toasts(Gfx g) {
		frameDelta = CLOCK.tick();
		var font = Minecraft.getInstance().font;
		int right = g.width() - 6;
		Iterator<Toast> it = TOASTS.iterator();
		int index = 0;
		while (it.hasNext()) {
			Toast t = it.next();
			t.slide.set(t.expired() ? 0 : 1);
			float s = t.slide.update(delta());
			if (t.expired() && s < 0.02f) {
				it.remove();
				continue;
			}
			if (t.slot.get() < 0) t.slot.snap(index);
			t.slot.set(index);
			float slot = t.slot.update(delta());
			index++;

			float ease = 1 - (1 - s) * (1 - s);
			int x = Math.round(right - CARD_W + (1 - ease) * (CARD_W + 12));
			int y = Math.round(6 + slot * (CARD_H + 5));
			int accent = t.entry == null ? Theme.ACCENT : t.entry.category.color;

			Draw.shadow(g, x, y, CARD_W, CARD_H, 7, 6, ease);
			Draw.panel(g, x, y, CARD_W, CARD_H, 7, 0xF2141822, 0xFF2A3142);
			Draw.circle(g, x + 17, y + CARD_H / 2f, 11, Draw.alpha(accent, 0.18f));
			if (t.entry != null) {
				g.item(ItemIcons.icon(t.entry), x + 9, y + CARD_H / 2 - 8);
			}
			g.text(font, Draw.ellipsize(font, t.heading, CARD_W - 42), x + 34, y + 7, accent, false);
			g.text(font, Draw.ellipsize(font, t.text, CARD_W - 42), x + 34, y + 19, Theme.TEXT, false);

			float life = Math.min(1, (System.currentTimeMillis() - t.born) / (float) LIFETIME_MS);
			int barW = Math.round((CARD_W - 14) * (1 - life));
			if (barW > 0) {
				g.fill(x + 7, y + CARD_H - 3, x + 7 + barW, y + CARD_H - 2, Draw.alpha(accent, 0.7f));
			}
		}
	}

	public static void compass(Gfx g, JournalEntry target, Nav nav) {
		var font = Minecraft.getInstance().font;
		COMPASS.set(1);
		float o = COMPASS.update(frameDelta == 0 ? 0.016f : frameDelta);

		String title = Draw.ellipsize(font, target.title().getString(), 130);
		String info;
		if (nav == null) {
			info = Component.translatable("chronicle.hud.other_dimension").getString();
		} else {
			info = EntryText.distance(nav.distance());
			if (Math.abs(nav.dy()) > 3) info += (nav.dy() > 0 ? "  ▲" : "  ▼") + Math.abs(nav.dy());
			if (nav.converted()) info += "  ×8";
		}
		int textW = Math.max(font.width(title), font.width(info));
		int w = 30 + textW + 10;
		int h = 26;
		int x = (g.width() - w) / 2;
		int y = Math.round(4 - (1 - o) * 10);

		Draw.shadow(g, x, y, w, h, h / 2f, 5, o);
		Draw.panel(g, x, y, w, h, h / 2f, 0xEE121620, 0xFF2A3142);
		float cx = x + 14, cy = y + h / 2f;
		Draw.circle(g, cx, cy, 9, nav == null ? 0x40FFFFFF : Draw.alpha(Theme.CYAN, 0.22f));
		if (nav != null) {
			Draw.arrow(g, cx, cy, 11, (float) Math.toRadians(nav.relativeAngle()), Theme.CYAN);
		} else {
			g.text(font, "?", Math.round(cx) - 2, Math.round(cy) - 4, Theme.TEXT_DIM, false);
		}
		g.text(font, title, x + 28, y + 4, Theme.TEXT, false);
		g.text(font, info, x + 28, y + 14, Theme.TEXT_DIM, false);
	}
}
