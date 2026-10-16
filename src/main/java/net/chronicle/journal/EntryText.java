package net.chronicle.journal;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/** Texte rund um Einträge, die beide UIs gleich anzeigen. */
public final class EntryText {
	private EntryText() {
	}

	public static String dimensionKey(String dimension) {
		return "chronicle.dimension." + dimension.replace(':', '.');
	}

	public static Component dimension(String dimension) {
		return Component.translatableWithFallback(dimensionKey(dimension), prettyId(dimension));
	}

	/** {@code minecraft:the_nether} → {@code The Nether} */
	public static String prettyId(String id) {
		String path = id.substring(id.indexOf(':') + 1);
		StringBuilder sb = new StringBuilder();
		for (String part : path.split("[_/]")) {
			if (part.isEmpty()) continue;
			if (!sb.isEmpty()) sb.append(' ');
			sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
		}
		return sb.toString();
	}

	public static String coords(JournalEntry e) {
		return e.x + " / " + e.y + " / " + e.z;
	}

	/** Entsprechende Koordinaten in der Partnerdimension (Oberwelt ↔ Nether), sonst {@code null}. */
	public static Component portalCoords(JournalEntry e) {
		if (EventTracker.OVERWORLD.equals(e.dimension)) {
			return Component.translatable("chronicle.detail.nether_coords", Math.floorDiv(e.x, 8), Math.floorDiv(e.z, 8));
		}
		if (EventTracker.NETHER.equals(e.dimension)) {
			return Component.translatable("chronicle.detail.overworld_coords", e.x * 8, e.z * 8);
		}
		return null;
	}

	public static String duration(long millis) {
		long minutes = Math.max(0, millis / 60000);
		if (minutes < 60) return minutes + " min";
		long hours = minutes / 60;
		return hours + " h " + (minutes % 60) + " min";
	}

	public static Component relativeTime(long time) {
		long seconds = Math.max(0, (System.currentTimeMillis() - time) / 1000);
		if (seconds < 60) return Component.translatable("chronicle.time.now");
		long minutes = seconds / 60;
		if (minutes < 60) return Component.translatable("chronicle.time.minutes", minutes);
		long hours = minutes / 60;
		if (hours < 24) return Component.translatable("chronicle.time.hours", hours);
		long days = hours / 24;
		if (days < 30) return Component.translatable("chronicle.time.days", days);
		return Component.literal(date(time));
	}

	public static String date(long time) {
		String pattern = Component.translatable("chronicle.date_format").getString();
		try {
			return DateTimeFormatter.ofPattern(pattern, Locale.ROOT).format(Instant.ofEpochMilli(time).atZone(ZoneId.systemDefault()));
		} catch (IllegalArgumentException e) {
			return DateTimeFormatter.ISO_LOCAL_DATE.format(Instant.ofEpochMilli(time).atZone(ZoneId.systemDefault()));
		}
	}

	public static String distance(double blocks) {
		if (blocks >= 10000) return String.format(Locale.ROOT, "%.1fk", blocks / 1000);
		return Math.round(blocks) + " m";
	}

	/** Pfeil + Entfernung, oder "hier" wenn man praktisch daneben steht. */
	public static String direction(Nav nav) {
		if (nav.distance() < 3 && !nav.converted()) return Component.translatable("chronicle.here").getString();
		return nav.arrow() + " " + distance(nav.distance());
	}

	/** "vor 5 Min. · Oberwelt · ↗ 120 m" */
	public static Component subtitle(JournalEntry e) {
		MutableComponent c = Component.empty().append(relativeTime(e.time)).append(" · ").append(dimension(e.dimension));
		LocalPlayer player = Minecraft.getInstance().player;
		Nav nav = player == null ? null : Nav.of(e, player);
		if (nav != null) {
			c.append(" · " + direction(nav));
		}
		return c;
	}

	/** Mehrzeiliger Detailtext für die Detailansicht. */
	public static Component details(JournalEntry e) {
		MutableComponent c = Component.empty();
		c.append(Component.translatable("chronicle.detail.category", e.category.label())).append("\n");
		c.append(Component.translatable("chronicle.detail.time", date(e.time), relativeTime(e.time))).append("\n");
		c.append(Component.translatable("chronicle.detail.dimension", dimension(e.dimension))).append("\n");
		c.append(Component.translatable("chronicle.detail.position", coords(e)));
		Component portal = portalCoords(e);
		if (portal != null) c.append("\n").append(portal);
		LocalPlayer player = Minecraft.getInstance().player;
		Nav nav = player == null ? null : Nav.of(e, player);
		if (nav != null) {
			c.append("\n").append(Component.translatable("chronicle.detail.distance", direction(nav)));
		}
		if (!e.text.isBlank()) {
			c.append("\n\n").append(Component.literal(e.text));
		}
		return c;
	}
}
