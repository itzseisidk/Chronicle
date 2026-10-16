package net.chronicle.journal;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import net.chronicle.compat.Platform;
import net.chronicle.config.ChronicleConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;

/** Erkennt rein clientseitig, was im Spiel passiert, und schreibt daraus Einträge. */
public final class EventTracker {
	public static final String OVERWORLD = "minecraft:overworld";
	public static final String NETHER = "minecraft:the_nether";
	public static final String END = "minecraft:the_end";

	/** Items, die auch mit Seltenheit "common" einen Eintrag wert sind. */
	private static final Set<String> NOTABLE_ITEMS = Set.of(
			"minecraft:iron_ingot", "minecraft:gold_ingot", "minecraft:diamond", "minecraft:emerald",
			"minecraft:ancient_debris", "minecraft:netherite_scrap", "minecraft:netherite_ingot",
			"minecraft:ender_pearl", "minecraft:ender_eye", "minecraft:blaze_rod", "minecraft:breeze_rod",
			"minecraft:shulker_shell", "minecraft:echo_shard", "minecraft:obsidian", "minecraft:trial_key",
			"minecraft:diamond_pickaxe", "minecraft:diamond_sword", "minecraft:saddle", "minecraft:name_tag");
	private static final int[] LEVELS = {10, 20, 30, 50, 100};
	private static final int[] DISTANCES = {1000, 5000, 10000, 25000};

	/** Schlüsselwort im Biom-Namen → Icon. Reihenfolge zählt (spezifische zuerst). */
	private static final Map<String, String> BIOME_ICONS = new LinkedHashMap<>();

	static {
		BIOME_ICONS.put("pale_garden", "minecraft:pale_oak_sapling");
		BIOME_ICONS.put("cherry", "minecraft:cherry_sapling");
		BIOME_ICONS.put("mushroom", "minecraft:red_mushroom");
		BIOME_ICONS.put("deep_dark", "minecraft:sculk");
		BIOME_ICONS.put("lush", "minecraft:glow_berries");
		BIOME_ICONS.put("dripstone", "minecraft:pointed_dripstone");
		BIOME_ICONS.put("badlands", "minecraft:terracotta");
		BIOME_ICONS.put("desert", "minecraft:sand");
		BIOME_ICONS.put("beach", "minecraft:sand");
		BIOME_ICONS.put("frozen", "minecraft:ice");
		BIOME_ICONS.put("ice", "minecraft:ice");
		BIOME_ICONS.put("snow", "minecraft:snowball");
		BIOME_ICONS.put("ocean", "minecraft:kelp");
		BIOME_ICONS.put("river", "minecraft:water_bucket");
		BIOME_ICONS.put("jungle", "minecraft:jungle_sapling");
		BIOME_ICONS.put("bamboo", "minecraft:bamboo");
		BIOME_ICONS.put("mangrove", "minecraft:mangrove_propagule");
		BIOME_ICONS.put("swamp", "minecraft:lily_pad");
		BIOME_ICONS.put("dark_forest", "minecraft:dark_oak_sapling");
		BIOME_ICONS.put("birch", "minecraft:birch_sapling");
		BIOME_ICONS.put("taiga", "minecraft:spruce_sapling");
		BIOME_ICONS.put("savanna", "minecraft:acacia_sapling");
		BIOME_ICONS.put("forest", "minecraft:oak_sapling");
		BIOME_ICONS.put("grove", "minecraft:spruce_sapling");
		BIOME_ICONS.put("flower", "minecraft:poppy");
		BIOME_ICONS.put("plains", "minecraft:dandelion");
		BIOME_ICONS.put("meadow", "minecraft:cornflower");
		BIOME_ICONS.put("peak", "minecraft:stone");
		BIOME_ICONS.put("slope", "minecraft:stone");
		BIOME_ICONS.put("windswept", "minecraft:stone");
		BIOME_ICONS.put("stony", "minecraft:stone");
		BIOME_ICONS.put("crimson", "minecraft:crimson_fungus");
		BIOME_ICONS.put("warped", "minecraft:warped_fungus");
		BIOME_ICONS.put("soul", "minecraft:soul_sand");
		BIOME_ICONS.put("basalt", "minecraft:basalt");
		BIOME_ICONS.put("nether", "minecraft:netherrack");
		BIOME_ICONS.put("end", "minecraft:end_stone");
	}

	private static int ticks;
	private static boolean wasDead;
	private static long aliveSince = System.currentTimeMillis();
	private static String lastDimension;
	private static JournalEntry lastDeath;
	private static long lastDeathTime;
	private static String lastDeathMessage;
	private static long lastDeathMessageTime;

	private EventTracker() {
	}

	public static void reset() {
		ticks = 0;
		wasDead = false;
		aliveSince = System.currentTimeMillis();
		lastDimension = null;
		lastDeath = null;
		lastDeathMessage = null;
	}

	public static void tick(Minecraft mc) {
		Journal journal = JournalManager.get();
		LocalPlayer player = mc.player;
		ClientLevel level = mc.level;
		if (journal == null || player == null || level == null) return;

		String dimension = Platform.dimensionId(level);
		if (!journal.started) {
			baseline(journal, player, level, dimension);
			return;
		}

		boolean dead = player.isDeadOrDying();
		if (dead && !wasDead) {
			onDeath(journal, player, dimension);
		} else if (!dead && wasDead) {
			aliveSince = System.currentTimeMillis();
		}
		wasDead = dead;
		if (dead) return;

		ticks++;
		if (ticks % 10 == 0) {
			checkDimension(journal, player, dimension);
			checkInventory(journal, player, dimension);
			checkMilestones(journal, player, dimension);
		}
		if (ticks % 20 == 5) {
			checkBiome(journal, player, level, dimension);
		}
	}

	/** Erster Besuch: alles, was der Spieler schon hat/kann, wird ohne Einträge übernommen. */
	private static void baseline(Journal journal, LocalPlayer player, ClientLevel level, String dimension) {
		journal.started = true;
		journal.startX = player.getBlockX();
		journal.startZ = player.getBlockZ();
		journal.dimensions.add(dimension);
		lastDimension = dimension;
		Inventory inventory = player.getInventory();
		for (int i = 0; i < inventory.getContainerSize(); i++) {
			ItemStack stack = inventory.getItem(i);
			if (!stack.isEmpty()) journal.items.add(ItemIcons.id(stack.getItem()));
		}
		for (int lvl : LEVELS) {
			if (player.experienceLevel >= lvl) journal.milestones.add("level_" + lvl);
		}
		// Wer schon tief unten (z. B. Superflat) oder hoch oben startet, bekommt dafür keinen Meilenstein.
		int y = player.getBlockY();
		if (OVERWORLD.equals(dimension) && y < 0) journal.milestones.add("depth_0");
		if (OVERWORLD.equals(dimension) && y <= -58) journal.milestones.add("bedrock");
		if (OVERWORLD.equals(dimension) && y >= 256) journal.milestones.add("sky");
		if (NETHER.equals(dimension) && y >= 128) journal.milestones.add("nether_roof");
		JournalEntry start = JournalEntry.of(Category.MILESTONE, "start")
				.subject(journal.worldName)
				.icon("minecraft:map")
				.at(dimension, player.getBlockX(), player.getBlockY(), player.getBlockZ());
		JournalManager.add(start, false);
	}

	private static void onDeath(Journal journal, LocalPlayer player, String dimension) {
		journal.deaths++;
		long now = System.currentTimeMillis();
		if (!ChronicleConfig.get().tracks(Category.DEATH)) {
			JournalManager.changed();
			return;
		}
		String detail = Component.translatable("chronicle.death.detail", player.experienceLevel, EntryText.duration(now - aliveSince)).getString();
		if (lastDeathMessage != null && now - lastDeathMessageTime < 5000) {
			detail = lastDeathMessage + "\n" + detail;
			lastDeathMessage = null;
		}
		JournalEntry entry = JournalEntry.of(Category.DEATH, "death")
				.subject(String.valueOf(journal.deaths))
				.text(detail)
				.at(dimension, player.getBlockX(), player.getBlockY(), player.getBlockZ());
		lastDeath = entry;
		lastDeathTime = now;
		JournalManager.add(entry, true);
	}

	/** Todesnachrichten kommen als Systemnachricht – wir hängen sie an den passenden Eintrag. */
	public static void onGameMessage(Component message) {
		if (!(message.getContents() instanceof TranslatableContents contents) || !contents.getKey().startsWith("death.")) return;
		LocalPlayer player = Minecraft.getInstance().player;
		Object[] args = contents.getArgs();
		if (player == null || args.length == 0) return;
		String victim = args[0] instanceof Component c ? c.getString() : String.valueOf(args[0]);
		if (!victim.equals(player.getName().getString())) return;

		long now = System.currentTimeMillis();
		if (lastDeath != null && now - lastDeathTime < 5000) {
			lastDeath.text = message.getString() + "\n" + lastDeath.text;
			lastDeath = null;
			JournalManager.changed();
		} else {
			lastDeathMessage = message.getString();
			lastDeathMessageTime = now;
		}
	}

	private static void checkDimension(Journal journal, LocalPlayer player, String dimension) {
		if (dimension.equals(lastDimension)) return;
		lastDimension = dimension;
		if (!journal.dimensions.add(dimension)) return;
		if (!ChronicleConfig.get().tracks(Category.DIMENSION)) {
			JournalManager.changed();
			return;
		}
		String icon = switch (dimension) {
			case NETHER -> "minecraft:netherrack";
			case END -> "minecraft:end_stone";
			case OVERWORLD -> "minecraft:grass_block";
			default -> "minecraft:obsidian";
		};
		JournalEntry entry = JournalEntry.of(Category.DIMENSION, "dimension")
				.subjectKey(EntryText.dimensionKey(dimension), EntryText.prettyId(dimension))
				.icon(icon)
				.at(dimension, player.getBlockX(), player.getBlockY(), player.getBlockZ());
		JournalManager.add(entry, true);
	}

	private static void checkBiome(Journal journal, LocalPlayer player, ClientLevel level, String dimension) {
		String biome = level.getBiome(player.blockPosition()).getRegisteredName();
		if (biome == null || biome.startsWith("[") || !journal.biomes.add(biome)) return;
		if (!ChronicleConfig.get().tracks(Category.BIOME)) {
			JournalManager.changed();
			return;
		}
		int colon = biome.indexOf(':');
		String namespace = colon < 0 ? "minecraft" : biome.substring(0, colon);
		String path = biome.substring(colon + 1);
		JournalEntry entry = JournalEntry.of(Category.BIOME, "biome")
				.subjectKey("biome." + namespace + "." + path, EntryText.prettyId(path))
				.icon(biomeIcon(path))
				.at(dimension, player.getBlockX(), player.getBlockY(), player.getBlockZ());
		JournalManager.add(entry, true);
	}

	private static String biomeIcon(String path) {
		for (Map.Entry<String, String> e : BIOME_ICONS.entrySet()) {
			if (path.contains(e.getKey())) return e.getValue();
		}
		return Category.BIOME.icon;
	}

	private static void checkInventory(Journal journal, LocalPlayer player, String dimension) {
		Inventory inventory = player.getInventory();
		boolean track = ChronicleConfig.get().tracks(Category.ITEM);
		for (int i = 0; i < inventory.getContainerSize(); i++) {
			ItemStack stack = inventory.getItem(i);
			if (stack.isEmpty()) continue;
			String id = ItemIcons.id(stack.getItem());
			if (!journal.items.add(id)) continue;
			if (!track || !(stack.getRarity() != Rarity.COMMON || NOTABLE_ITEMS.contains(id))) {
				JournalManager.changed();
				continue;
			}
			JournalEntry entry = JournalEntry.of(Category.ITEM, "item")
					.subjectKey(stack.getItem().getDescriptionId(), stack.getHoverName().getString())
					.icon(id)
					.at(dimension, player.getBlockX(), player.getBlockY(), player.getBlockZ());
			JournalManager.add(entry, true);
		}
	}

	private static void checkMilestones(Journal journal, LocalPlayer player, String dimension) {
		if (!ChronicleConfig.get().tracks(Category.MILESTONE)) return;
		int x = player.getBlockX(), y = player.getBlockY(), z = player.getBlockZ();

		for (int lvl : LEVELS) {
			if (player.experienceLevel >= lvl && journal.milestones.add("level_" + lvl)) {
				milestone(dimension, x, y, z, "level", String.valueOf(lvl), "minecraft:experience_bottle");
			}
		}
		if (OVERWORLD.equals(dimension)) {
			if (y < 0 && journal.milestones.add("depth_0")) {
				milestone(dimension, x, y, z, "depth", "0", "minecraft:deepslate");
			}
			if (y <= -58 && journal.milestones.add("bedrock")) {
				milestone(dimension, x, y, z, "bedrock", "", "minecraft:bedrock");
			}
			if (y >= 256 && journal.milestones.add("sky")) {
				milestone(dimension, x, y, z, "sky", "256", "minecraft:feather");
			}
			double dx = x - journal.startX, dz = z - journal.startZ;
			double distance = Math.sqrt(dx * dx + dz * dz);
			for (int d : DISTANCES) {
				if (distance >= d && journal.milestones.add("dist_" + d)) {
					milestone(dimension, x, y, z, "distance", String.valueOf(d), "minecraft:compass");
				}
			}
		} else if (NETHER.equals(dimension) && y >= 128 && journal.milestones.add("nether_roof")) {
			milestone(dimension, x, y, z, "nether_roof", "", "minecraft:bedrock");
		}
	}

	private static void milestone(String dimension, int x, int y, int z, String type, String subject, String icon) {
		JournalEntry entry = JournalEntry.of(Category.MILESTONE, type).subject(subject).icon(icon).at(dimension, x, y, z);
		JournalManager.add(entry, true);
	}
}
