package net.chronicle.compat;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * Basis für alle Chronicle-Screens. Leitet das versionsabhängige Rendering
 * auf {@link #draw} und {@link #drawBackground} mit {@link Gfx} um.
 */
public abstract class CScreen extends InputScreen {
	protected CScreen(Component title) {
		super(title);
	}

	/** Standard: Vanilla-Hintergrund (Blur bzw. Menü-Hintergrund). */
	protected void drawBackground(Gfx g, int mouseX, int mouseY, float partialTick) {
		vanillaBackground(g, mouseX, mouseY, partialTick);
	}

	/** Standard: nur die registrierten Vanilla-Widgets zeichnen. */
	protected void draw(Gfx g, int mouseX, int mouseY, float partialTick) {
		drawWidgets(g, mouseX, mouseY, partialTick);
	}

	protected final void vanillaBackground(Gfx g, int mouseX, int mouseY, float partialTick) {
		super.renderBackground(g.raw(), mouseX, mouseY, partialTick);
	}

	protected final void drawWidgets(Gfx g, int mouseX, int mouseY, float partialTick) {
		super.render(g.raw(), mouseX, mouseY, partialTick);
	}

	@Override
	public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		drawBackground(new Gfx(g), mouseX, mouseY, partialTick);
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		draw(new Gfx(g), mouseX, mouseY, partialTick);
	}
}
