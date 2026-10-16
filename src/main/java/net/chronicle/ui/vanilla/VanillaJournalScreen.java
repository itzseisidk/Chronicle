package net.chronicle.ui.vanilla;

import java.util.Arrays;
import net.chronicle.compat.CScreen;
import net.chronicle.compat.Gfx;
import net.chronicle.compat.Platform;
import net.chronicle.config.ChronicleConfig;
import net.chronicle.journal.Filter;
import net.chronicle.journal.Journal;
import net.chronicle.journal.JournalEntry;
import net.chronicle.journal.JournalManager;
import net.chronicle.journal.Notes;
import net.chronicle.ui.JournalScreens;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

/** Die Vanilla-UI: nur Minecraft-eigene Komponenten (Button, EditBox, CycleButton, Auswahlliste). */
public final class VanillaJournalScreen extends CScreen {
	private final Screen parent;
	private Filter filter = Filter.ALL;
	private String search = "";
	private String selectedId;
	private int seenRevision = -1;

	private EntryList list;
	private Button detailsButton, targetButton, pinButton, deleteButton;

	public VanillaJournalScreen(Screen parent) {
		super(Component.translatable("chronicle.title"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		int rowW = Math.min(width - 20, 380);
		int left = (width - rowW) / 2;

		int filterW = 120, noteW = 90;
		int searchW = rowW - filterW - noteW - 8;
		EditBox searchBox = new EditBox(font, left + 1, 23, searchW - 2, 18, Component.translatable("chronicle.search"));
		searchBox.setHint(Component.translatable("chronicle.search_hint"));
		searchBox.setMaxLength(64);
		searchBox.setValue(search);
		searchBox.setResponder(s -> {
			search = s;
			refresh();
		});
		addRenderableWidget(searchBox);
		addRenderableWidget(Platform.cycle(Filter::label, Arrays.asList(Filter.values()), filter,
				left + searchW + 4, 22, filterW, 20, Component.translatable("chronicle.filter"), (button, value) -> {
					filter = value;
					refresh();
				}));
		addRenderableWidget(Button.builder(Component.translatable("chronicle.button.new_note"),
				b -> Platform.setScreen(new VanillaNoteScreen(this, null))).bounds(left + rowW - noteW, 22, noteW, 20).build());

		list = new EntryList(minecraft, width, height - 48 - 60, 48, this::openDetails);
		list.setRowWidth(rowW);
		addRenderableWidget(list);

		int y1 = height - 52;
		int bw = (rowW - 12) / 4;
		detailsButton = addRenderableWidget(Button.builder(Component.translatable("chronicle.button.details"),
				b -> withSelected(this::openDetails)).bounds(left, y1, bw, 20).build());
		targetButton = addRenderableWidget(Button.builder(Component.translatable("chronicle.button.target"),
				b -> withSelected(Notes::toggleTarget)).bounds(left + (bw + 4), y1, bw, 20).build());
		pinButton = addRenderableWidget(Button.builder(Component.translatable("chronicle.button.pin"),
				b -> withSelected(Notes::togglePin)).bounds(left + (bw + 4) * 2, y1, bw, 20).build());
		deleteButton = addRenderableWidget(Button.builder(Component.translatable("chronicle.button.delete"),
				b -> withSelected(this::confirmDelete)).bounds(left + (bw + 4) * 3, y1, rowW - (bw + 4) * 3, 20).build());

		int y2 = height - 28;
		int bw2 = (rowW - 8) / 3;
		addRenderableWidget(Button.builder(Component.translatable("chronicle.button.settings"),
				b -> Platform.setScreen(new VanillaSettingsScreen(this))).bounds(left, y2, bw2, 20).build());
		Button style = Button.builder(Component.translatable("chronicle.button.style", ChronicleConfig.UiStyle.VANILLA.label()),
				b -> JournalScreens.switchStyle(ChronicleConfig.UiStyle.MODERN, parent)).bounds(left + bw2 + 4, y2, bw2, 20).build();
		style.setTooltip(Tooltip.create(Component.translatable("chronicle.style.switch_tooltip")));
		addRenderableWidget(style);
		addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> onClose()).bounds(left + (bw2 + 4) * 2, y2, rowW - (bw2 + 4) * 2, 20).build());

		refresh();
		setInitialFocus(searchBox);
	}

	private void refresh() {
		if (list == null) return;
		seenRevision = JournalManager.revision();
		JournalEntry selected = list.selectedEntry();
		if (selected != null) selectedId = selected.id;
		list.setEntries(Filter.query(JournalManager.get(), filter, search), selectedId);
		updateButtons();
	}

	private void updateButtons() {
		JournalEntry e = list.selectedEntry();
		boolean has = e != null;
		detailsButton.active = has;
		targetButton.active = has;
		pinButton.active = has;
		deleteButton.active = has;
		targetButton.setMessage(Component.translatable(has && JournalManager.isTarget(e) ? "chronicle.button.untarget" : "chronicle.button.target"));
		pinButton.setMessage(Component.translatable(has && e.pinned ? "chronicle.button.unpin" : "chronicle.button.pin"));
	}

	private void withSelected(java.util.function.Consumer<JournalEntry> action) {
		JournalEntry e = list.selectedEntry();
		if (e != null) {
			selectedId = e.id;
			action.accept(e);
		}
	}

	private void openDetails(JournalEntry entry) {
		selectedId = entry.id;
		Platform.setScreen(new VanillaEntryScreen(this, entry));
	}

	private void confirmDelete(JournalEntry entry) {
		Platform.setScreen(new ConfirmScreen(confirmed -> {
			if (confirmed) JournalManager.remove(entry);
			Platform.setScreen(this);
		}, Component.translatable("chronicle.delete.title"), Component.translatable("chronicle.delete.message", entry.title())));
	}

	@Override
	public void tick() {
		if (seenRevision != JournalManager.revision()) refresh();
		JournalEntry e = list.selectedEntry();
		if (e != null) selectedId = e.id;
		updateButtons();
	}

	@Override
	protected void draw(Gfx g, int mouseX, int mouseY, float partialTick) {
		drawWidgets(g, mouseX, mouseY, partialTick);
		Journal journal = JournalManager.get();
		Component heading = journal == null || journal.worldName.isEmpty()
				? title
				: Component.translatable("chronicle.title_world", title, journal.worldName);
		g.centered(font, heading, width / 2, 8, 0xFFFFFFFF);
		if (list.children().isEmpty()) {
			int cy = 48 + (height - 108) / 2 - 10;
			g.centered(font, Component.translatable("chronicle.empty.title"), width / 2, cy, 0xFFFFFFFF);
			g.centered(font, Component.translatable("chronicle.empty.hint_short"), width / 2, cy + 12, 0xFFA0A0A0);
		}
	}

	@Override
	public void onClose() {
		Platform.setScreen(parent);
	}
}
