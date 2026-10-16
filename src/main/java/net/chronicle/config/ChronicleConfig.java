package net.chronicle.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import net.chronicle.ChronicleClient;
import net.chronicle.journal.Category;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.chat.Component;

public final class ChronicleConfig {
	public enum UiStyle {
		VANILLA, MODERN;

		public Component label() {
			return Component.translatable("chronicle.style." + name().toLowerCase(java.util.Locale.ROOT));
		}
	}

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static ChronicleConfig instance = new ChronicleConfig();

	public UiStyle style = UiStyle.MODERN;
	public boolean notifications = true;
	public boolean compass = true;
	public boolean sounds = true;
	public List<Category> disabled = new ArrayList<>();

	public static ChronicleConfig get() {
		return instance;
	}

	public boolean tracks(Category category) {
		return !disabled.contains(category);
	}

	public void setTracked(Category category, boolean tracked) {
		disabled.remove(category);
		if (!tracked) {
			disabled.add(category);
		}
	}

	private static Path path() {
		return FabricLoader.getInstance().getConfigDir().resolve("chronicle.json");
	}

	public static void load() {
		Path path = path();
		if (!Files.exists(path)) {
			save();
			return;
		}
		try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
			ChronicleConfig loaded = GSON.fromJson(reader, ChronicleConfig.class);
			if (loaded != null) {
				if (loaded.style == null) loaded.style = UiStyle.MODERN;
				if (loaded.disabled == null) loaded.disabled = new ArrayList<>();
				loaded.disabled.removeIf(c -> c == null);
				instance = loaded;
			}
		} catch (Exception e) {
			ChronicleClient.LOG.warn("Could not read chronicle.json, using defaults", e);
		}
	}

	public static void save() {
		try {
			Files.createDirectories(path().getParent());
			try (Writer writer = Files.newBufferedWriter(path(), StandardCharsets.UTF_8)) {
				GSON.toJson(instance, writer);
			}
		} catch (IOException e) {
			ChronicleClient.LOG.warn("Could not save chronicle.json", e);
		}
	}
}
