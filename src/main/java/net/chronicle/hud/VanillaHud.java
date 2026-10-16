package net.chronicle.hud;

import net.chronicle.compat.Gfx;
import net.chronicle.journal.EntryText;
import net.chronicle.journal.JournalEntry;
import net.chronicle.journal.Nav;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

final class VanillaHud {
	private VanillaHud() {
	}

	static void compass(Gfx g, JournalEntry target, Nav nav) {
		var font = Minecraft.getInstance().font;
		String title = font.plainSubstrByWidth(target.title().getString(), 140);
		MutableComponent line;
		if (nav == null) {
			line = Component.literal("◎ " + title + " · ").append(Component.translatable("chronicle.hud.other_dimension"))
					.withStyle(ChatFormatting.GRAY);
		} else {
			line = Component.literal(nav.arrow() + " ").withStyle(ChatFormatting.YELLOW)
					.append(Component.literal(title).withStyle(ChatFormatting.WHITE))
					.append(Component.literal(" · " + EntryText.distance(nav.distance())).withStyle(ChatFormatting.GRAY));
			int dy = nav.dy();
			if (Math.abs(dy) > 3) line.append(Component.literal((dy > 0 ? " ▲" : " ▼") + Math.abs(dy)).withStyle(ChatFormatting.GRAY));
			if (nav.converted()) line.append(Component.literal(" (×8)").withStyle(ChatFormatting.DARK_GRAY));
		}
		g.centered(font, line, g.width() / 2, 4, 0xFFFFFFFF);
	}
}
