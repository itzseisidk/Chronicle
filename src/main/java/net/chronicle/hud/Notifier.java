package net.chronicle.hud;

import net.chronicle.compat.Platform;
import net.chronicle.config.ChronicleConfig;
import net.chronicle.journal.JournalEntry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;

public final class Notifier {
	private static final SystemToast.SystemToastId TOAST = new SystemToast.SystemToastId(4000L);

	private Notifier() {
	}

	public static void entryAdded(JournalEntry entry) {
		show(Component.translatable("chronicle.toast." + entry.category.key()), entry.title(), entry);
	}

	public static void show(Component heading, Component text, JournalEntry entry) {
		ChronicleConfig cfg = ChronicleConfig.get();
		if (!cfg.notifications) return;
		if (cfg.style == ChronicleConfig.UiStyle.VANILLA) {
			SystemToast.add(Platform.toasts(), TOAST, heading, text);
		} else {
			ModernHud.push(heading, text, entry);
			if (cfg.sounds) {
				Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_CHIME.value(), 1.9f, 0.3f));
			}
		}
	}
}
