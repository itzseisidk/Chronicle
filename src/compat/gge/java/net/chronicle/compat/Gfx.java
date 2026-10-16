package net.chronicle.compat;

import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;

public final class Gfx {
	private final GuiGraphicsExtractor g;

	public Gfx(GuiGraphicsExtractor g) {
		this.g = g;
	}

	GuiGraphicsExtractor raw() {
		return g;
	}

	public int width() {
		return g.guiWidth();
	}

	public int height() {
		return g.guiHeight();
	}

	public void fill(int x1, int y1, int x2, int y2, int color) {
		g.fill(x1, y1, x2, y2, color);
	}

	public void gradient(int x1, int y1, int x2, int y2, int top, int bottom) {
		g.fillGradient(x1, y1, x2, y2, top, bottom);
	}

	public void text(Font font, Component text, int x, int y, int color, boolean shadow) {
		g.text(font, text, x, y, color, shadow);
	}

	public void text(Font font, String text, int x, int y, int color, boolean shadow) {
		g.text(font, text, x, y, color, shadow);
	}

	public void text(Font font, FormattedCharSequence text, int x, int y, int color, boolean shadow) {
		g.text(font, text, x, y, color, shadow);
	}

	public void centered(Font font, Component text, int centerX, int y, int color) {
		g.centeredText(font, text, centerX, y, color);
	}

	public void item(ItemStack stack, int x, int y) {
		g.item(stack, x, y);
	}

	public void scissor(int x1, int y1, int x2, int y2) {
		g.enableScissor(x1, y1, x2, y2);
	}

	public void endScissor() {
		g.disableScissor();
	}

	public void push() {
		g.pose().pushMatrix();
	}

	public void pop() {
		g.pose().popMatrix();
	}

	public void translate(float x, float y) {
		g.pose().translate(x, y);
	}

	public void scale(float s) {
		g.pose().scale(s, s);
	}

	public void rotate(float radians) {
		g.pose().rotate(radians);
	}

	// alles danach liegt ueber items und text
	public void layer() {
		g.nextStratum();
	}

	public void tooltip(Font font, Component text, int x, int y) {
		g.setTooltipForNextFrame(font, text, x, y);
	}

	public void tooltip(Font font, List<FormattedCharSequence> lines, int x, int y) {
		g.setTooltipForNextFrame(font, lines, x, y);
	}
}
