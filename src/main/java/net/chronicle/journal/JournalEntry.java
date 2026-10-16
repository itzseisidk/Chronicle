package net.chronicle.journal;

import java.util.UUID;
import net.minecraft.network.chat.Component;

public final class JournalEntry {
	public String id = UUID.randomUUID().toString();
	public Category category = Category.NOTE;
	public String type = "note";
	// bei notizen ist subject der titel, sonst das argument fuer chronicle.entry.<type>
	public String subject = "";
	public boolean subjectKey;
	public String subjectFallback = "";
	public String text = "";
	public String icon = "";
	public long time = System.currentTimeMillis();
	public String dimension = "minecraft:overworld";
	public int x, y, z;
	public boolean pinned;

	public static JournalEntry of(Category category, String type) {
		JournalEntry e = new JournalEntry();
		e.category = category;
		e.type = type;
		return e;
	}

	public JournalEntry subject(String subject) {
		this.subject = subject;
		this.subjectKey = false;
		return this;
	}

	public JournalEntry subjectKey(String key, String fallback) {
		this.subject = key;
		this.subjectKey = true;
		this.subjectFallback = fallback;
		return this;
	}

	public JournalEntry text(String text) {
		this.text = text;
		return this;
	}

	public JournalEntry icon(String icon) {
		this.icon = icon;
		return this;
	}

	public JournalEntry at(String dimension, int x, int y, int z) {
		this.dimension = dimension;
		this.x = x;
		this.y = y;
		this.z = z;
		return this;
	}

	public Component subjectComponent() {
		return subjectKey ? Component.translatableWithFallback(subject, subjectFallback) : Component.literal(subject);
	}

	public Component title() {
		if (category == Category.NOTE) {
			return subject.isBlank() ? Component.translatable("chronicle.note.untitled") : Component.literal(subject);
		}
		return Component.translatable("chronicle.entry." + type, subjectComponent());
	}

	public String iconId() {
		return icon == null || icon.isEmpty() ? category.icon : icon;
	}
}
