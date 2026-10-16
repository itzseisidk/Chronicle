package net.chronicle.journal;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Das Tagebuch einer Welt bzw. eines Servers. Wird als JSON gespeichert. */
public final class Journal {
	public int format = 1;
	public String worldName = "";
	public long created = System.currentTimeMillis();
	public List<JournalEntry> entries = new ArrayList<>();

	/** Bereits Gesehenes – damit jedes Biom/Item/jede Dimension nur einmal geloggt wird. */
	public Set<String> biomes = new HashSet<>();
	public Set<String> items = new HashSet<>();
	public Set<String> dimensions = new HashSet<>();
	public Set<String> milestones = new HashSet<>();

	public int deaths;
	public String targetId;
	/** {@code false} bis zum ersten Tick in dieser Welt (dann wird der Ist-Zustand still übernommen). */
	public boolean started;
	public int startX, startZ;

	public JournalEntry find(String id) {
		if (id == null) return null;
		for (JournalEntry entry : entries) {
			if (entry.id.equals(id)) return entry;
		}
		return null;
	}

	public JournalEntry target() {
		return find(targetId);
	}

	public int count(Category category) {
		int n = 0;
		for (JournalEntry entry : entries) {
			if (entry.category == category) n++;
		}
		return n;
	}

	public int pinnedCount() {
		int n = 0;
		for (JournalEntry entry : entries) {
			if (entry.pinned) n++;
		}
		return n;
	}

	/** Nach dem Laden: fehlende Felder aus alten/kaputten Dateien auffüllen. */
	void repair() {
		if (entries == null) entries = new ArrayList<>();
		entries.removeIf(e -> e == null || e.category == null);
		if (biomes == null) biomes = new HashSet<>();
		if (items == null) items = new HashSet<>();
		if (dimensions == null) dimensions = new HashSet<>();
		if (milestones == null) milestones = new HashSet<>();
		if (worldName == null) worldName = "";
		for (JournalEntry e : entries) {
			if (e.id == null) e.id = java.util.UUID.randomUUID().toString();
			if (e.type == null) e.type = "note";
			if (e.subject == null) e.subject = "";
			if (e.subjectFallback == null) e.subjectFallback = "";
			if (e.text == null) e.text = "";
			if (e.icon == null) e.icon = "";
			if (e.dimension == null) e.dimension = "minecraft:overworld";
		}
	}
}
