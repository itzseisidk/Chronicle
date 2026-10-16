package net.chronicle.ui.vanilla;

import java.util.Arrays;
import net.chronicle.compat.CScreen;
import net.chronicle.compat.Gfx;
import net.chronicle.compat.Platform;
import net.chronicle.config.ChronicleConfig;
import net.chronicle.journal.Category;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

/** Einstellungen im klassischen Optionen-Layout: zwei Spalten mit Ein/Aus-Buttons. */
final class VanillaSettingsScreen extends CScreen {
	private final Screen parent;
	private int trackingY;

	VanillaSettingsScreen(Screen parent) {
		super(Component.translatable("chronicle.settings.title"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		ChronicleConfig cfg = ChronicleConfig.get();
		int colW = 150;
		int left = width / 2 - colW - 5;
		int right = width / 2 + 5;
		int y = 32;

		addRenderableWidget(CycleButton.onOffBuilder(cfg.notifications).create(left, y, colW, 20,
				Component.translatable("chronicle.settings.notifications"), (b, v) -> {
					cfg.notifications = v;
					ChronicleConfig.save();
				}));
		addRenderableWidget(CycleButton.onOffBuilder(cfg.compass).create(right, y, colW, 20,
				Component.translatable("chronicle.settings.compass"), (b, v) -> {
					cfg.compass = v;
					ChronicleConfig.save();
				}));
		y += 24;
		addRenderableWidget(CycleButton.onOffBuilder(cfg.sounds).create(left, y, colW, 20,
				Component.translatable("chronicle.settings.sounds"), (b, v) -> {
					cfg.sounds = v;
					ChronicleConfig.save();
				}));
		addRenderableWidget(Platform.cycle(ChronicleConfig.UiStyle::label, Arrays.asList(ChronicleConfig.UiStyle.values()), cfg.style,
				right, y, colW, 20, Component.translatable("chronicle.settings.style"), (b, v) -> {
					cfg.style = v;
					ChronicleConfig.save();
				}));

		y += 34;
		trackingY = y;
		y += 14;
		Category[] categories = Category.values();
		for (int i = 0; i < categories.length; i++) {
			Category c = categories[i];
			int x = i % 2 == 0 ? left : right;
			addRenderableWidget(CycleButton.onOffBuilder(cfg.tracks(c)).create(x, y + (i / 2) * 24, colW, 20, c.label(), (b, v) -> {
				cfg.setTracked(c, v);
				ChronicleConfig.save();
			}));
		}

		addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> onClose()).bounds(width / 2 - 100, height - 28, 200, 20).build());
	}

	@Override
	protected void draw(Gfx g, int mouseX, int mouseY, float partialTick) {
		drawWidgets(g, mouseX, mouseY, partialTick);
		g.centered(font, title, width / 2, 12, 0xFFFFFFFF);
		g.centered(font, Component.translatable("chronicle.settings.tracking"), width / 2, trackingY, 0xFFA0A0A0);
	}

	@Override
	public void onClose() {
		if (parent instanceof VanillaJournalScreen && ChronicleConfig.get().style == ChronicleConfig.UiStyle.MODERN) {
			Platform.setScreen(net.chronicle.ui.JournalScreens.create(null));
			return;
		}
		Platform.setScreen(parent);
	}
}
