package net.chronicle.ui.modern.widget;

import net.chronicle.compat.Gfx;
import net.chronicle.config.ChronicleConfig;
import net.chronicle.ui.modern.Anim;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;

/** Basis aller Modern-Komponenten. Bewusst unabhängig von Vanillas Widget-System. */
public abstract class MWidget {
	public int x, y, w, h;
	public boolean visible = true;
	public boolean enabled = true;
	public Component tooltip;
	protected boolean focused;
	protected final Anim hover = new Anim(0, 16);

	@SuppressWarnings("unchecked")
	public <T extends MWidget> T bounds(int x, int y, int w, int h) {
		this.x = x;
		this.y = y;
		this.w = w;
		this.h = h;
		return (T) this;
	}

	public boolean contains(double mx, double my) {
		return visible && mx >= x && mx < x + w && my >= y && my < y + h;
	}

	public final void render(Gfx g, int mx, int my, float dt) {
		if (!visible) return;
		hover.set(enabled && contains(mx, my) ? 1 : 0);
		hover.update(dt);
		draw(g, mx, my, dt);
	}

	protected abstract void draw(Gfx g, int mx, int my, float dt);

	public boolean mouseDown(double mx, double my, int button) {
		return false;
	}

	public boolean mouseUp(double mx, double my, int button) {
		return false;
	}

	public boolean mouseDrag(double mx, double my, int button, double dx, double dy) {
		return false;
	}

	public boolean scroll(double mx, double my, double amount) {
		return false;
	}

	public boolean key(int key, int modifiers) {
		return false;
	}

	public boolean charTyped(int codepoint) {
		return false;
	}

	public boolean focusable() {
		return false;
	}

	public boolean isFocused() {
		return focused;
	}

	public void setFocused(boolean focused) {
		this.focused = focused;
	}

	protected static Font font() {
		return Minecraft.getInstance().font;
	}

	public static void click() {
		if (ChronicleConfig.get().sounds) {
			Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.value(), 1.25f, 0.35f));
		}
	}
}
