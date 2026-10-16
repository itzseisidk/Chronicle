package net.chronicle.compat;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

public abstract class CScreen extends InputScreen {
	protected CScreen(Component title) {
		super(title);
	}

	protected void drawBackground(Gfx g, int mouseX, int mouseY, float partialTick) {
		vanillaBackground(g, mouseX, mouseY, partialTick);
	}

	protected void draw(Gfx g, int mouseX, int mouseY, float partialTick) {
		drawWidgets(g, mouseX, mouseY, partialTick);
	}

	protected final void vanillaBackground(Gfx g, int mouseX, int mouseY, float partialTick) {
		super.extractBackground(g.raw(), mouseX, mouseY, partialTick);
	}

	protected final void drawWidgets(Gfx g, int mouseX, int mouseY, float partialTick) {
		super.extractRenderState(g.raw(), mouseX, mouseY, partialTick);
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
		drawBackground(new Gfx(g), mouseX, mouseY, partialTick);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
		draw(new Gfx(g), mouseX, mouseY, partialTick);
	}
}
