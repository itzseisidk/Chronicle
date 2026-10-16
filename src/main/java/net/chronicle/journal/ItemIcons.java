package net.chronicle.journal;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Item-IDs (als String) ↔ Items, ohne vom ID-Typ der jeweiligen Version abzuhängen. */
public final class ItemIcons {
	private static Map<String, Item> byId;
	private static final Map<String, ItemStack> STACKS = new HashMap<>();

	private ItemIcons() {
	}

	public static String id(Item item) {
		return BuiltInRegistries.ITEM.getKey(item).toString();
	}

	public static ItemStack stack(String id) {
		return STACKS.computeIfAbsent(id, key -> {
			if (byId == null) {
				byId = new HashMap<>();
				for (Item item : BuiltInRegistries.ITEM) {
					byId.put(id(item), item);
				}
			}
			Item item = byId.get(key);
			return item == null ? ItemStack.EMPTY : new ItemStack(item);
		});
	}

	/** Icon des Eintrags, mit Kategorie-Icon als Ersatz falls das Item (z. B. aus einem entfernten Mod) fehlt. */
	public static ItemStack icon(JournalEntry entry) {
		ItemStack stack = stack(entry.iconId());
		return stack.isEmpty() ? stack(entry.category.icon) : stack;
	}
}
