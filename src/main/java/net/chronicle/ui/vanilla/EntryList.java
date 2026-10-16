package net.chronicle.ui.vanilla;

import java.util.List;
import java.util.function.Consumer;
import net.chronicle.compat.CList;
import net.chronicle.compat.Gfx;
import net.chronicle.journal.EntryText;
import net.chronicle.journal.ItemIcons;
import net.chronicle.journal.JournalEntry;
import net.chronicle.journal.JournalManager;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

final class EntryList extends CList<EntryList.EntryRow> {
	private final Consumer<JournalEntry> onOpen;

	EntryList(Minecraft mc, int width, int height, int y, Consumer<JournalEntry> onOpen) {
		super(mc, width, height, y, 26);
		this.onOpen = onOpen;
	}

	void setEntries(List<JournalEntry> entries, String selectedId) {
		clear();
		for (JournalEntry e : entries) {
			EntryRow row = new EntryRow(e);
			add(row);
			if (e.id.equals(selectedId)) setSelected(row);
		}
	}

	JournalEntry selectedEntry() {
		EntryRow row = getSelected();
		return row == null ? null : row.entry;
	}

	final class EntryRow extends CList.Row<EntryRow> {
		final JournalEntry entry;

		EntryRow(JournalEntry entry) {
			this.entry = entry;
		}

		@Override
		protected void draw(Gfx g, int x, int y, int width, int height, int mouseX, int mouseY, boolean hovered) {
			var font = Minecraft.getInstance().font;
			g.item(ItemIcons.icon(entry), x + 2, y + (height - 16) / 2);

			int right = x + width - 4;
			if (JournalManager.isTarget(entry)) {
				right -= font.width("◎");
				g.text(font, "◎", right, y + 2, 0xFF55FFFF, true);
				right -= 4;
			}
			if (entry.pinned) {
				right -= font.width("★");
				g.text(font, "★", right, y + 2, 0xFFFFAA00, true);
				right -= 4;
			}
			int textW = right - (x + 24);
			g.text(font, font.plainSubstrByWidth(entry.title().getString(), textW), x + 24, y + 2, 0xFFFFFFFF, true);
			g.text(font, font.plainSubstrByWidth(EntryText.subtitle(entry).getString(), x + width - 4 - (x + 24)), x + 24, y + 13, 0xFF9A9A9A, false);
		}

		@Override
		protected boolean clicked(double mouseX, double mouseY, int button, boolean doubleClick) {
			if (button == 0 && doubleClick) {
				setSelected(this);
				onOpen.accept(entry);
				return true;
			}
			return false;
		}

		@Override
		public Component getNarration() {
			return Component.translatable("narrator.select", entry.title());
		}
	}
}
