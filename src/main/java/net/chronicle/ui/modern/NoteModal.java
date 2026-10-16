package net.chronicle.ui.modern;

import net.chronicle.compat.Gfx;
import net.chronicle.journal.JournalEntry;
import net.chronicle.journal.Notes;
import net.chronicle.ui.modern.widget.MButton;
import net.chronicle.ui.modern.widget.MModal;
import net.chronicle.ui.modern.widget.MTextArea;
import net.chronicle.ui.modern.widget.MTextField;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

final class NoteModal extends MModal {
	private final JournalEntry existing;
	private String titleText;
	private String bodyText;
	private MTextField titleField;
	private MTextArea bodyArea;

	NoteModal(JournalEntry existing) {
		super(Component.translatable(existing == null ? "chronicle.note.new" : "chronicle.note.edit"), 320, 200);
		this.existing = existing;
		this.titleText = existing == null ? "" : existing.subject;
		this.bodyText = existing == null ? "" : existing.text;
	}

	@Override
	protected void build() {
		int pad = 12;
		titleField = add(new MTextField("✎").bounds(x + pad, y + 32, w - pad * 2, 20));
		titleField.maxLength(64).placeholder(Component.translatable("chronicle.note.title_hint")).onChange(s -> titleText = s);
		titleField.setValue(titleText);

		int bodyH = h - 60 - 50;
		bodyArea = add(new MTextArea().bounds(x + pad, y + 60, w - pad * 2, bodyH));
		bodyArea.maxLength(1000).placeholder(Component.translatable("chronicle.note.text_hint")).onChange(s -> bodyText = s);
		bodyArea.setValue(bodyText);

		int by = y + h - 30;
		add(new MButton(Component.translatable("chronicle.button.save"), "✓", MButton.Style.PRIMARY, this::submit).bounds(x + w - pad - 90, by, 90, 20));
		add(new MButton(Component.translatable("gui.cancel"), null, MButton.Style.GHOST, this::close).bounds(x + w - pad - 90 - 6 - 80, by, 80, 20));
		focus(existing == null ? titleField : bodyArea);
	}

	@Override
	protected void drawContent(Gfx g, int mx, int my, float dt) {
		if (existing == null) {
			var font = Minecraft.getInstance().font;
			String hint = Draw.ellipsize(font, Component.translatable("chronicle.note.position_hint").getString(), w - 24 - 60);
			g.text(font, "⌖ " + hint, x + 12, bodyArea.y + bodyArea.h + 4, Theme.TEXT_MUTED, false);
		}
	}

	@Override
	protected void submit() {
		// komplett leer = einfach zumachen, keine leere notiz speichern
		if (!titleText.isBlank() || !bodyText.isBlank()) {
			if (existing == null) Notes.create(titleText, bodyText);
			else Notes.update(existing, titleText, bodyText);
		}
		close();
	}
}
