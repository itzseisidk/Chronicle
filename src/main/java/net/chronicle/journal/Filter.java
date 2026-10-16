package net.chronicle.journal;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import net.minecraft.network.chat.Component;

/** Filter für die Eintragsliste – von beiden UIs genutzt. */
public enum Filter {
	ALL(null),
	PINNED(null),
	BIOME(Category.BIOME),
	DIMENSION(Category.DIMENSION),
	DEATH(Category.DEATH),
	ITEM(Category.ITEM),
	MILESTONE(Category.MILESTONE),
	NOTE(Category.NOTE);

	public final Category category;

	Filter(Category category) {
		this.category = category;
	}

	public Component label() {
		return category != null ? category.label() : Component.translatable("chronicle.filter." + name().toLowerCase(Locale.ROOT));
	}

	public int color() {
		return category != null ? category.color : this == PINNED ? 0xFFFACC15 : 0xFF94A3B8;
	}

	public boolean test(JournalEntry e) {
		return switch (this) {
			case ALL -> true;
			case PINNED -> e.pinned;
			default -> e.category == category;
		};
	}

	public int count(Journal journal) {
		return switch (this) {
			case ALL -> journal.entries.size();
			case PINNED -> journal.pinnedCount();
			default -> journal.count(category);
		};
	}

	/** Gefilterte Einträge, neueste zuerst. */
	public static List<JournalEntry> query(Journal journal, Filter filter, String search) {
		List<JournalEntry> result = new ArrayList<>();
		if (journal == null) return result;
		String needle = search == null ? "" : search.trim().toLowerCase(Locale.ROOT);
		for (JournalEntry e : journal.entries) {
			if (!filter.test(e)) continue;
			if (!needle.isEmpty()) {
				String haystack = (e.title().getString() + " " + e.text + " " + EntryText.coords(e) + " "
						+ EntryText.dimension(e.dimension).getString()).toLowerCase(Locale.ROOT);
				if (!haystack.contains(needle)) continue;
			}
			result.add(e);
		}
		result.sort(Comparator.comparingLong((JournalEntry e) -> e.time).reversed());
		return result;
	}
}
