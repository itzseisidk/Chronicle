package net.chronicle.journal;

import net.chronicle.compat.Platform;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

public final class Notes {
	private Notes() {
	}

	public static JournalEntry create(String title, String text) {
		LocalPlayer player = Minecraft.getInstance().player;
		JournalEntry entry = JournalEntry.of(Category.NOTE, "note").subject(title.trim()).text(text.trim());
		if (player != null) {
			entry.at(Platform.dimensionId(player.level()), player.getBlockX(), player.getBlockY(), player.getBlockZ());
		}
		JournalManager.add(entry, false);
		return entry;
	}

	public static void update(JournalEntry entry, String title, String text) {
		entry.subject = title.trim();
		entry.text = text.trim();
		JournalManager.changed();
	}

	public static void togglePin(JournalEntry entry) {
		entry.pinned = !entry.pinned;
		JournalManager.changed();
	}

	public static void toggleTarget(JournalEntry entry) {
		JournalManager.setTarget(JournalManager.isTarget(entry) ? null : entry);
	}

	public static void copyCoords(JournalEntry entry) {
		Minecraft.getInstance().keyboardHandler.setClipboard(entry.x + " " + entry.y + " " + entry.z);
	}
}
