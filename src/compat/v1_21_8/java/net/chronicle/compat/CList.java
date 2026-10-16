package net.chronicle.compat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ObjectSelectionList;

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
		private static final long DOUBLE_CLICK_MS = 250;
		private long lastClick;

		// nur der inhalt, den auswahlrahmen macht vanilla
		protected abstract void draw(Gfx g, int x, int y, int width, int height, int mouseX, int mouseY, boolean hovered);

		protected boolean clicked(double mouseX, double mouseY, int button, boolean doubleClick) {
			return false;
		}

		@Override
		public void render(GuiGraphics g, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean hovered, float partialTick) {
			draw(new Gfx(g), left, top, width, height, mouseX, mouseY, hovered);
		}

		@Override
		public boolean mouseClicked(double mouseX, double mouseY, int button) {
			long now = System.currentTimeMillis();
			boolean doubleClick = now - lastClick < DOUBLE_CLICK_MS;
			lastClick = now;
			return clicked(mouseX, mouseY, button, doubleClick) || super.mouseClicked(mouseX, mouseY, button);
		}
	}
}
