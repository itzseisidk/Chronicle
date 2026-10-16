package net.chronicle.journal;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import net.chronicle.ChronicleClient;
import net.chronicle.hud.Notifier;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.world.level.storage.LevelResource;

/**
 * Hält das Tagebuch der aktuellen Welt. Pro Einzelspielerwelt bzw. Serveradresse
 * gibt es eine eigene Datei unter {@code .minecraft/chronicle/}.
 */
public final class JournalManager {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final long SAVE_DELAY_MS = 3000;

	private static Journal journal;
	private static Path file;
	private static boolean dirty;
	private static long lastChange;
	/** Wird bei jeder Änderung erhöht, damit offene Screens wissen, dass sie neu laden müssen. */
	private static int revision;

	private JournalManager() {
	}

	public static Journal get() {
		return journal;
	}

	public static int revision() {
		return revision;
	}

	public static void tick(Minecraft mc) {
		if (journal == null && mc.player != null && mc.level != null) {
			load(mc);
		}
		if (dirty && System.currentTimeMillis() - lastChange > SAVE_DELAY_MS) {
			save();
		}
	}

	public static void add(JournalEntry entry, boolean notify) {
		if (journal == null) return;
		journal.entries.add(entry);
		changed();
		if (notify) {
			Notifier.entryAdded(entry);
		}
	}

	public static void remove(JournalEntry entry) {
		if (journal == null) return;
		journal.entries.remove(entry);
		if (entry.id.equals(journal.targetId)) {
			journal.targetId = null;
		}
		changed();
	}

	public static void setTarget(JournalEntry entry) {
		if (journal == null) return;
		journal.targetId = entry == null ? null : entry.id;
		changed();
	}

	public static boolean isTarget(JournalEntry entry) {
		return journal != null && entry != null && entry.id.equals(journal.targetId);
	}

	public static void changed() {
		dirty = true;
		lastChange = System.currentTimeMillis();
		revision++;
	}

	public static void unload() {
		if (journal != null && dirty) {
			save();
		}
		journal = null;
		file = null;
		dirty = false;
		revision++;
		EventTracker.reset();
	}

	private static void load(Minecraft mc) {
		String[] id = worldId(mc);
		file = FabricLoader.getInstance().getGameDir().resolve("chronicle").resolve(id[0] + ".json");
		Journal loaded = null;
		if (Files.exists(file)) {
			try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
				loaded = GSON.fromJson(reader, Journal.class);
			} catch (Exception e) {
				ChronicleClient.LOG.error("Tagebuch {} ist beschädigt, lege Sicherung an", file, e);
				try {
					Files.copy(file, file.resolveSibling(file.getFileName() + ".broken"), StandardCopyOption.REPLACE_EXISTING);
				} catch (Exception ignored) {
				}
			}
		}
		if (loaded == null) {
			loaded = new Journal();
		}
		loaded.repair();
		loaded.worldName = id[1];
		journal = loaded;
		dirty = false;
		revision++;
		ChronicleClient.LOG.info("Chronik geladen: {} ({} Einträge)", file.getFileName(), journal.entries.size());
	}

	public static void save() {
		if (journal == null || file == null) return;
		try {
			Files.createDirectories(file.getParent());
			Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
			try (Writer writer = Files.newBufferedWriter(tmp, StandardCharsets.UTF_8)) {
				GSON.toJson(journal, writer);
			}
			Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
			dirty = false;
		} catch (Exception e) {
			ChronicleClient.LOG.error("Konnte Tagebuch nicht speichern", e);
		}
	}

	/** @return {Dateischlüssel, Anzeigename} */
	private static String[] worldId(Minecraft mc) {
		IntegratedServer server = mc.getSingleplayerServer();
		if (server != null) {
			Path root = server.getWorldPath(LevelResource.ROOT).toAbsolutePath().normalize();
			String folder = root.getFileName() == null ? "world" : root.getFileName().toString();
			return new String[] {"sp_" + sanitize(folder), server.getWorldData().getLevelName()};
		}
		ServerData data = mc.getCurrentServer();
		if (data != null) {
			return new String[] {"mp_" + sanitize(data.ip), data.name};
		}
		return new String[] {"unknown", "?"};
	}

	private static String sanitize(String s) {
		String clean = s.toLowerCase(java.util.Locale.ROOT).replaceAll("[^a-z0-9._-]", "_");
		return clean.isEmpty() ? "world" : clean;
	}
}
