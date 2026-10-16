package net.chronicle.ui.modern;

import java.util.List;
import net.chronicle.compat.Gfx;
import net.chronicle.ui.modern.widget.MButton;
import net.chronicle.ui.modern.widget.MModal;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

final class ConfirmModal extends MModal {
	private final Component message;
	private final Component confirmLabel;
	private final Runnable onConfirm;

	ConfirmModal(Component title, Component message, Component confirmLabel, Runnable onConfirm) {
		super(title, 280, 112);
		this.message = message;
		this.confirmLabel = confirmLabel;
		this.onConfirm = onConfirm;
	}

	@Override
	protected void build() {
		int by = y + h - 30;
		add(new MButton(confirmLabel, "✖", MButton.Style.DANGER, this::submit).bounds(x + w - 12 - 96, by, 96, 20));
		add(new MButton(Component.translatable("gui.cancel"), null, MButton.Style.GHOST, this::close).bounds(x + w - 12 - 96 - 6 - 80, by, 80, 20));
	}

	@Override
	protected void drawContent(Gfx g, int mx, int my, float dt) {
		var font = Minecraft.getInstance().font;
		List<FormattedCharSequence> lines = font.split(message, w - 24);
		for (int i = 0; i < Math.min(3, lines.size()); i++) {
			g.text(font, lines.get(i), x + 12, y + 32 + i * 10, Theme.TEXT_DIM, false);
		}
	}

	@Override
	protected void submit() {
		onConfirm.run();
		close();
	}
}
