package net.chronicle.ui.modern.widget;

import net.chronicle.compat.Gfx;
import net.chronicle.ui.modern.Anim;
import net.chronicle.ui.modern.Draw;
import net.chronicle.ui.modern.Theme;
import net.minecraft.network.chat.Component;

/** Abgerundeter Button mit optionalem Icon-Zeichen, Hover- und Klick-Animation. */
public class MButton extends MWidget {
	public enum Style { PRIMARY, SECONDARY, GHOST, DANGER }

	public Component label;
	public String icon;
	public Style style;
	public boolean active;
	public int iconColor;
	private final Runnable onClick;
	private final Anim press = new Anim(0, 20);

	public MButton(Component label, String icon, Style style, Runnable onClick) {
		this.label = label;
		this.icon = icon;
		this.style = style;
		this.onClick = onClick;
	}

	public static MButton icon(String icon, Component tooltip, Runnable onClick) {
		MButton b = new MButton(null, icon, Style.GHOST, onClick);
		b.tooltip = tooltip;
		return b;
	}

	@Override
	protected void draw(Gfx g, int mx, int my, float dt) {
		float hv = hover.get();
		float pr = press.update(dt);
		press.set(0);

		int base, hot, text;
		switch (active ? Style.PRIMARY : style) {
			case PRIMARY -> {
				base = Theme.ACCENT;
				hot = Theme.ACCENT_HOVER;
				text = 0xFFFFFFFF;
			}
			case DANGER -> {
				base = 0x33EF4444;
				hot = Theme.DANGER;
				text = hv > 0.5f ? 0xFFFFFFFF : Theme.DANGER_HOVER;
			}
			case GHOST -> {
				base = 0x00000000;
				hot = Theme.SURFACE_HOVER;
				text = Draw.lerp(Theme.TEXT_DIM, Theme.TEXT, hv);
			}
			default -> {
				base = Theme.SURFACE_ACTIVE;
				hot = 0xFF36405A;
				text = Theme.TEXT;
			}
		}
		int bg = Draw.lerp(base, hot, hv);
		if (!enabled) {
			bg = Draw.alpha(bg, 0.4f);
			text = Theme.TEXT_MUTED;
		}
		float inset = pr * 1.0f;
		Draw.round(g, x + inset, y + inset, w - inset * 2, h - inset * 2, Math.min(5, h / 2f), bg);

		String iconText = icon == null ? "" : icon;
		String labelText = label == null ? "" : label.getString();
		int gap = !iconText.isEmpty() && !labelText.isEmpty() ? 4 : 0;
		int iconW = font().width(iconText);
		int maxLabel = w - 8 - iconW - gap;
		labelText = Draw.ellipsize(font(), labelText, Math.max(0, maxLabel));
		int total = iconW + gap + font().width(labelText);
		int tx = x + (w - total) / 2;
		int ty = y + (h - 8) / 2;
		if (!iconText.isEmpty()) {
			g.text(font(), iconText, tx, ty, iconColor != 0 && enabled ? iconColor : text, false);
		}
		if (!labelText.isEmpty()) {
			g.text(font(), labelText, tx + iconW + gap, ty, text, false);
		}
	}

	@Override
	public boolean mouseDown(double mx, double my, int button) {
		if (button != 0 || !enabled || !contains(mx, my)) return false;
		press.snap(1);
		click();
		onClick.run();
		return true;
	}
}
