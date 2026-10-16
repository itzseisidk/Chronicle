package net.chronicle.ui.modern;

import net.chronicle.compat.Gfx;
import net.chronicle.config.ChronicleConfig;
import net.chronicle.journal.Category;
import net.chronicle.ui.modern.widget.MButton;
import net.chronicle.ui.modern.widget.MModal;
import net.chronicle.ui.modern.widget.MToggle;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

final class SettingsModal extends MModal {
	private int trackingY;

	SettingsModal() {
		super(Component.translatable("chronicle.settings.title"), 340, 208);
	}

	@Override
	protected void build() {
		ChronicleConfig cfg = ChronicleConfig.get();
		int pad = 12;
		int rowH = 18;
		int ty = y + 30;
		add(new MToggle(Component.translatable("chronicle.settings.notifications"), null, cfg.notifications, v -> {
			cfg.notifications = v;
			ChronicleConfig.save();
		}).bounds(x + pad, ty, w - pad * 2, rowH));
		ty += rowH + 2;
		add(new MToggle(Component.translatable("chronicle.settings.compass"), null, cfg.compass, v -> {
			cfg.compass = v;
			ChronicleConfig.save();
		}).bounds(x + pad, ty, w - pad * 2, rowH));
		ty += rowH + 2;
		add(new MToggle(Component.translatable("chronicle.settings.sounds"), null, cfg.sounds, v -> {
			cfg.sounds = v;
			ChronicleConfig.save();
		}).bounds(x + pad, ty, w - pad * 2, rowH));
		ty += rowH + 2;

		trackingY = ty + 8;
		int colW = (w - pad * 2 - 6) / 2;
		Category[] categories = Category.values();
		for (int i = 0; i < categories.length; i++) {
			Category c = categories[i];
			int cx = x + pad + (i % 2) * (colW + 6);
			int cy = trackingY + 14 + (i / 2) * (rowH + 2);
			add(new MToggle(c.label(), c.color, cfg.tracks(c), v -> {
				cfg.setTracked(c, v);
				ChronicleConfig.save();
			}).bounds(cx, cy, colW, rowH));
		}
		add(new MButton(Component.translatable("gui.done"), null, MButton.Style.PRIMARY, this::close).bounds(x + w - pad - 80, y + h - 28, 80, 20));
	}

	@Override
	protected void drawContent(Gfx g, int mx, int my, float dt) {
		var font = Minecraft.getInstance().font;
		Component label = Component.translatable("chronicle.settings.tracking");
		g.text(font, label, x + 12, trackingY + 2, Theme.TEXT_MUTED, false);
		g.fill(x + 12 + font.width(label) + 6, trackingY + 6, x + w - 12, trackingY + 7, Theme.DIVIDER);
	}

	@Override
	protected void submit() {
		close();
	}
}
