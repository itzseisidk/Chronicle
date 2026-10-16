package net.chronicle.ui.vanilla;

import net.chronicle.compat.CScreen;
import net.chronicle.compat.Gfx;
import net.chronicle.compat.Platform;
import net.chronicle.journal.JournalEntry;
import net.chronicle.journal.Notes;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

/** Notiz anlegen/bearbeiten mit Vanillas EditBox und MultiLineEditBox. */
public final class VanillaNoteScreen extends CScreen {
	private final Screen parent;
	private final JournalEntry existing;
	private String titleText;
	private String bodyText;

	public VanillaNoteScreen(Screen parent, JournalEntry existing) {
		super(Component.translatable(existing == null ? "chronicle.note.new" : "chronicle.note.edit"));
		this.parent = parent;
		this.existing = existing;
		this.titleText = existing == null ? "" : existing.subject;
		this.bodyText = existing == null ? "" : existing.text;
	}

	@Override
	protected void init() {
		int rowW = Math.min(width - 20, 320);
		int left = (width - rowW) / 2;

		EditBox titleBox = new EditBox(font, left + 1, 45, rowW - 2, 18, Component.translatable("chronicle.note.title"));
		titleBox.setMaxLength(64);
		titleBox.setHint(Component.translatable("chronicle.note.title_hint"));
		titleBox.setValue(titleText);
		titleBox.setResponder(s -> titleText = s);
		addRenderableWidget(titleBox);

		int bodyH = Math.max(40, height - 82 - 60);
		MultiLineEditBox body = MultiLineEditBox.builder()
				.setX(left)
				.setY(82)
				.setPlaceholder(Component.translatable("chronicle.note.text_hint"))
				.build(font, rowW, bodyH, Component.translatable("chronicle.note.text"));
		body.setCharacterLimit(1000);
		body.setValue(bodyText);
		body.setValueListener(s -> bodyText = s);
		addRenderableWidget(body);

		int y = height - 28;
		int bw = (rowW - 4) / 2;
		addRenderableWidget(Button.builder(Component.translatable("chronicle.button.save"), b -> save()).bounds(left, y, bw, 20).build());
		addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, b -> onClose()).bounds(left + bw + 4, y, rowW - bw - 4, 20).build());
		setInitialFocus(existing == null ? titleBox : body);
	}

	private void save() {
		if (!(titleText.isBlank() && bodyText.isBlank())) {
			if (existing == null) Notes.create(titleText, bodyText);
			else Notes.update(existing, titleText, bodyText);
		}
		onClose();
	}

	@Override
	protected void draw(Gfx g, int mouseX, int mouseY, float partialTick) {
		drawWidgets(g, mouseX, mouseY, partialTick);
		int rowW = Math.min(width - 20, 320);
		int left = (width - rowW) / 2;
		g.centered(font, title, width / 2, 12, 0xFFFFFFFF);
		g.text(font, Component.translatable("chronicle.note.title"), left, 34, 0xFFA0A0A0, false);
		g.text(font, Component.translatable("chronicle.note.text"), left, 71, 0xFFA0A0A0, false);
		if (existing == null) {
			g.text(font, Component.translatable("chronicle.note.position_hint"), left, height - 42, 0xFF808080, false);
		}
	}

	@Override
	public void onClose() {
		Platform.setScreen(parent);
	}
}
