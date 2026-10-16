package net.chronicle.compat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.input.MouseButtonEvent;

/** Vanilla-Auswahlliste mit versionsneutralen Zeilen (ab 26.1). */
public abstract class CList<R extends CList.Row<R>> extends ObjectSelectionList<R> {
	private int rowWidth = 220;

	protected CList(Minecraft minecraft, int width, int height, int y, int itemHeight) {
		super(minecraft, width, height, y, itemHeight);
	}

	public void setRowWidth(int rowWidth) {
		this.rowWidth = rowWidth;
	}

	@Override
	public int getRowWidth() {
		return rowWidth;
	}

	public void add(R row) {
		addEntry(row);
	}

	public void clear() {
		clearEntries();
	}

	public abstract static class Row<R extends Row<R>> extends ObjectSelectionList.Entry<R> {
		/** Zeichnet den Inhalt der Zeile (ohne Auswahlrahmen, den macht Vanilla). */
		protected abstract void draw(Gfx g, int x, int y, int width, int height, int mouseX, int mouseY, boolean hovered);

		protected boolean clicked(double mouseX, double mouseY, int button, boolean doubleClick) {
			return false;
		}

		@Override
		public void extractContent(GuiGraphicsExtractor g, int mouseX, int mouseY, boolean hovered, float partialTick) {
			draw(new Gfx(g), getContentX(), getContentY(), getContentWidth(), getContentHeight(), mouseX, mouseY, hovered);
		}

		@Override
		public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
			return clicked(event.x(), event.y(), event.button(), doubleClick) || super.mouseClicked(event, doubleClick);
		}
	}
}
