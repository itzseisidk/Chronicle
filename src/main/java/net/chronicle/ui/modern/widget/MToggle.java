package net.chronicle.ui.modern.widget;

import java.util.function.Consumer;
import net.chronicle.compat.Gfx;
import net.chronicle.ui.modern.Anim;
import net.chronicle.ui.modern.Draw;
import net.chronicle.ui.modern.Theme;
import net.minecraft.network.chat.Component;

/** Beschriftung links, animierter Schalter rechts. */
public class MToggle extends MWidget {
	private final Component label;
	private final Integer dotColor;
	private boolean value;
	private final Consumer<Boolean> onChange;
	private final Anim knob;

	public MToggle(Component label, Integer dotColor, boolean value, Consumer<Boolean> onChange) {
		this.label = label;
		this.dotColor = dotColor;
		this.value = value;
		this.onChange = onChange;
		this.knob = new Anim(value ? 1 : 0, 18);
	}

	@Override
	protected void draw(Gfx g, int mx, int my, float dt) {
		float k = knob.update(dt);
		Draw.round(g, x, y, w, h, 5, Draw.alpha(Theme.SURFACE_HOVER, hover.get()));
		int tx = x + 6;
		if (dotColor != null) {
			Draw.circle(g, x + 9, y + h / 2f, 3, dotColor);
			tx = x + 17;
		}
		int sw = 22, sh = 12;
		int sx = x + w - sw - 5, sy = y + (h - sh) / 2;
		g.text(font(), Draw.ellipsize(font(), label.getString(), sx - tx - 4), tx, y + (h - 8) / 2, Theme.TEXT, false);
		Draw.round(g, sx, sy, sw, sh, sh / 2f, Draw.lerp(0xFF333C52, Theme.ACCENT, k));
		float kx = sx + 2 + (sw - sh) * k;
		Draw.round(g, kx, sy + 2, sh - 4, sh - 4, (sh - 4) / 2f, 0xFFFFFFFF);
	}

	@Override
	public boolean mouseDown(double mx, double my, int button) {
		if (button != 0 || !contains(mx, my)) return false;
		value = !value;
		knob.set(value ? 1 : 0);
		click();
		onChange.accept(value);
		return true;
	}
}
