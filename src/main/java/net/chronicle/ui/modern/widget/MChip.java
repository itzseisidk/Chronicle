package net.chronicle.ui.modern.widget;

import java.util.function.IntSupplier;
import net.chronicle.compat.Gfx;
import net.chronicle.journal.Filter;
import net.chronicle.ui.modern.Anim;
import net.chronicle.ui.modern.Draw;
import net.chronicle.ui.modern.Theme;

public class MChip extends MWidget {
	public final Filter filter;
	public boolean selected;
	public boolean compact;
	private final IntSupplier count;
	private final Runnable onClick;
	private final Anim select = new Anim(0, 16);

	public MChip(Filter filter, IntSupplier count, Runnable onClick) {
		this.filter = filter;
		this.count = count;
		this.onClick = onClick;
		this.tooltip = filter.label();
	}

	@Override
	protected void draw(Gfx g, int mx, int my, float dt) {
		select.set(selected ? 1 : 0);
		float s = select.update(dt);
		int bg = Draw.lerp(Draw.alpha(Theme.SURFACE_HOVER, hover.get()), Theme.SURFACE_ACTIVE, s);
		Draw.round(g, x, y, w, h, 5, bg);
		if (s > 0.01f) {
			Draw.round(g, x, y + 4, 2, h - 8, 1, Draw.alpha(filter.color(), s));
		}
		float cy = y + h / 2f;
		if (compact) {
			Draw.circle(g, x + w / 2f, cy, 3.5f, filter.color());
			return;
		}
		Draw.circle(g, x + 10, cy, 3, filter.color());
		String n = String.valueOf(count.getAsInt());
		int nw = font().width(n);
		int textColor = selected ? Theme.TEXT : Draw.lerp(Theme.TEXT_DIM, Theme.TEXT, hover.get());
		g.text(font(), Draw.ellipsize(font(), filter.label().getString(), w - 30 - nw), x + 18, y + (h - 8) / 2, textColor, false);
		Draw.round(g, x + w - nw - 9, y + 3, nw + 6, h - 6, (h - 6) / 2f, selected ? 0x30FFFFFF : 0x18FFFFFF);
		g.text(font(), n, x + w - nw - 6, y + (h - 8) / 2, Theme.TEXT_DIM, false);
	}

	@Override
	public boolean mouseDown(double mx, double my, int button) {
		if (button != 0 || !contains(mx, my)) return false;
		click();
		onClick.run();
		return true;
	}
}
