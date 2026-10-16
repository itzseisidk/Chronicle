package net.chronicle.ui.vanilla;

import net.chronicle.compat.CScreen;
import net.chronicle.compat.Gfx;
import net.chronicle.compat.Platform;
import net.chronicle.journal.Category;
import net.chronicle.journal.EntryText;
import net.chronicle.journal.ItemIcons;
import net.chronicle.journal.JournalEntry;
import net.chronicle.journal.JournalManager;
import net.chronicle.journal.Notes;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineTextWidget;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

/** Detailansicht eines Eintrags (Vanilla). */
final class VanillaEntryScreen extends CScreen {
	private final Screen parent;
	private final JournalEntry entry;
	private MultiLineTextWidget details;
	private Button targetButton, pinButton, copyButton;
	private int ticks;
	private int copiedTicks;

	VanillaEntryScreen(Screen parent, JournalEntry entry) {
		super(entry.title());
		this.parent = parent;
		this.entry = entry;
	}

	@Override
	protected void init() {
		int rowW = Math.min(width - 20, 340);
		int left = (width - rowW) / 2;

		details = new MultiLineTextWidget(left + 24, 36, EntryText.details(entry), font);
		details.setMaxWidth(rowW - 24);
		addRenderableWidget(details);

		int y1 = height - 52;
		int bw = (rowW - 8) / 3;
		targetButton = addRenderableWidget(Button.builder(Component.empty(), b -> Notes.toggleTarget(entry)).bounds(left, y1, bw, 20).build());
		copyButton = addRenderableWidget(Button.builder(Component.translatable("chronicle.button.copy"), b -> {
			Notes.copyCoords(entry);
			copiedTicks = 30;
		}).bounds(left + bw + 4, y1, bw, 20).build());
		pinButton = addRenderableWidget(Button.builder(Component.empty(), b -> Notes.togglePin(entry))
				.bounds(left + (bw + 4) * 2, y1, rowW - (bw + 4) * 2, 20).build());

		int y2 = height - 28;
		boolean note = entry.category == Category.NOTE;
		int count = note ? 3 : 2;
		int bw2 = (rowW - 4 * (count - 1)) / count;
		int x = left;
		if (note) {
			addRenderableWidget(Button.builder(Component.translatable("chronicle.button.edit"),
					b -> Platform.setScreen(new VanillaNoteScreen(this, entry))).bounds(x, y2, bw2, 20).build());
			x += bw2 + 4;
		}
		addRenderableWidget(Button.builder(Component.translatable("chronicle.button.delete"), b -> confirmDelete()).bounds(x, y2, bw2, 20).build());
		x += bw2 + 4;
		addRenderableWidget(Button.builder(CommonComponents.GUI_BACK, b -> onClose()).bounds(x, y2, left + rowW - x, 20).build());
		updateLabels();
	}

	private void confirmDelete() {
		Platform.setScreen(new ConfirmScreen(confirmed -> {
			if (confirmed) {
				JournalManager.remove(entry);
				Platform.setScreen(parent);
			} else {
				Platform.setScreen(this);
			}
		}, Component.translatable("chronicle.delete.title"), Component.translatable("chronicle.delete.message", entry.title())));
	}

	private void updateLabels() {
		targetButton.setMessage(Component.translatable(JournalManager.isTarget(entry) ? "chronicle.button.untarget" : "chronicle.button.target"));
		pinButton.setMessage(Component.translatable(entry.pinned ? "chronicle.button.unpin" : "chronicle.button.pin"));
		copyButton.setMessage(Component.translatable(copiedTicks > 0 ? "chronicle.button.copied" : "chronicle.button.copy"));
	}

	@Override
	public void tick() {
		if (copiedTicks > 0) copiedTicks--;
		if (++ticks % 10 == 0) {
			details.setMessage(EntryText.details(entry));
		}
		updateLabels();
	}

	@Override
	protected void draw(Gfx g, int mouseX, int mouseY, float partialTick) {
		drawWidgets(g, mouseX, mouseY, partialTick);
		int rowW = Math.min(width - 20, 340);
		int left = (width - rowW) / 2;
		g.centered(font, entry.title(), width / 2, 12, 0xFFFFFFFF);
		g.item(ItemIcons.icon(entry), left, 34);
	}

	@Override
	public void onClose() {
		Platform.setScreen(parent);
	}
}
