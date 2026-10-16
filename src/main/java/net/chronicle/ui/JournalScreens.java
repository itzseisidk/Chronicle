package net.chronicle.ui;

import net.chronicle.compat.Platform;
import net.chronicle.config.ChronicleConfig;
import net.chronicle.journal.JournalManager;
import net.chronicle.ui.modern.ModernJournalScreen;
import net.chronicle.ui.vanilla.VanillaJournalScreen;
import net.chronicle.ui.vanilla.VanillaNoteScreen;
import net.minecraft.client.gui.screens.Screen;

/** Öffnet je nach eingestelltem Stil die Vanilla- oder die Modern-UI. */
public final class JournalScreens {
	private JournalScreens() {
	}

	public static void open() {
		if (JournalManager.get() == null) return;
		Platform.setScreen(create(null));
	}

	public static void openNewNote() {
		if (JournalManager.get() == null) return;
		if (ChronicleConfig.get().style == ChronicleConfig.UiStyle.MODERN) {
			Platform.setScreen(ModernJournalScreen.withNewNote(null));
		} else {
			Platform.setScreen(new VanillaNoteScreen(null, null));
		}
	}

	public static Screen create(Screen parent) {
		return ChronicleConfig.get().style == ChronicleConfig.UiStyle.MODERN
				? new ModernJournalScreen(parent)
				: new VanillaJournalScreen(parent);
	}

	public static void switchStyle(ChronicleConfig.UiStyle style, Screen parent) {
		ChronicleConfig.get().style = style;
		ChronicleConfig.save();
		Platform.setScreen(create(parent));
	}
}
