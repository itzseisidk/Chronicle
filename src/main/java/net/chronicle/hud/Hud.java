package net.chronicle.hud;

import net.chronicle.compat.Gfx;
import net.chronicle.compat.Platform;
import net.chronicle.config.ChronicleConfig;
import net.chronicle.journal.Journal;
import net.chronicle.journal.JournalEntry;
import net.chronicle.journal.JournalManager;
import net.chronicle.journal.Nav;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/** HUD-Einstieg: Kompass zum Ziel und (Modern) die Benachrichtigungskarten. */
public final class Hud {
	private Hud() {
	}

	public static void tick() {
		Minecraft mc = Minecraft.getInstance();
		Journal journal = JournalManager.get();
		if (mc.player == null || journal == null) return;
		JournalEntry target = journal.target();
		if (target == null) return;
		Nav nav = Nav.of(target, mc.player);
		if (nav != null && !nav.converted() && nav.distance() < 3 && Math.abs(nav.dy()) < 4) {
			JournalManager.setTarget(null);
			Notifier.show(Component.translatable("chronicle.toast.reached"), target.title(), target);
		}
	}

	public static void render(Gfx g) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || Platform.hudHidden()) return;
		ChronicleConfig cfg = ChronicleConfig.get();
		boolean modern = cfg.style == ChronicleConfig.UiStyle.MODERN;
		Journal journal = JournalManager.get();
		if (journal != null && cfg.compass) {
			JournalEntry target = journal.target();
			Nav nav = target == null ? null : Nav.of(target, mc.player);
			if (target != null) {
				if (modern) ModernHud.compass(g, target, nav);
				else VanillaHud.compass(g, target, nav);
			} else {
				ModernHud.hideCompass();
			}
		}
		if (modern) ModernHud.toasts(g);
	}
}
