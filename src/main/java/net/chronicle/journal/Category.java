package net.chronicle.journal;

import java.util.Locale;
import net.minecraft.network.chat.Component;

/** Art eines Tagebuch-Eintrags. Farbe und Icon werden von beiden UIs genutzt. */
public enum Category {
	BIOME(0xFF4ADE80, "minecraft:grass_block"),
	DIMENSION(0xFFA78BFA, "minecraft:obsidian"),
	DEATH(0xFFF87171, "minecraft:skeleton_skull"),
	ITEM(0xFF38BDF8, "minecraft:diamond"),
	MILESTONE(0xFFFBBF24, "minecraft:experience_bottle"),
	NOTE(0xFFF472B6, "minecraft:writable_book");

	public final int color;
	public final String icon;

	Category(int color, String icon) {
		this.color = color;
		this.icon = icon;
	}

	public String key() {
		return name().toLowerCase(Locale.ROOT);
	}

	public Component label() {
		return Component.translatable("chronicle.category." + key());
	}
}
