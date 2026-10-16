package net.chronicle.ui.modern;

import net.chronicle.compat.Gfx;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

public final class Draw {
	private Draw() {
	}

	private static float guiScale() {
		return Math.max(1, Minecraft.getInstance().getWindow().getGuiScale());
	}

	// draws in real pixels instead of gui pixels, otherwise the corners look super jaggy at gui scale 3-4.
	// the edge gets one half transparent pixel per row as cheap antialiasing
	public static void round(Gfx g, float x, float y, float w, float h, float radius, int color) {
		if (w <= 0 || h <= 0 || (color >>> 24) == 0) return;
		float s = guiScale();
		int px = Math.round(x * s);
		int py = Math.round(y * s);
		int pw = Math.round((x + w) * s) - px;
		int ph = Math.round((y + h) * s) - py;
		int r = Math.min(Math.round(radius * s), Math.min(pw, ph) / 2);

		g.push();
		g.scale(1f / s);
		if (r <= 0) {
			g.fill(px, py, px + pw, py + ph, color);
			g.pop();
			return;
		}
		g.fill(px, py + r, px + pw, py + ph - r, color);
		int alpha = color >>> 24;
		int rgb = color & 0xFFFFFF;
		for (int i = 0; i < r; i++) {
			double dy = r - i - 0.5;
			double edge = r - Math.sqrt((double) r * r - dy * dy);
			int full = (int) Math.ceil(edge);
			float coverage = (float) (full - edge);
			int top = py + i;
			int bottom = py + ph - 1 - i;
			g.fill(px + full, top, px + pw - full, top + 1, color);
			g.fill(px + full, bottom, px + pw - full, bottom + 1, color);
			if (coverage > 0.05f && full > 0) {
				int aa = ((int) (alpha * coverage) << 24) | rgb;
				g.fill(px + full - 1, top, px + full, top + 1, aa);
				g.fill(px + pw - full, top, px + pw - full + 1, top + 1, aa);
				g.fill(px + full - 1, bottom, px + full, bottom + 1, aa);
				g.fill(px + pw - full, bottom, px + pw - full + 1, bottom + 1, aa);
			}
		}
		g.pop();
	}

	public static void panel(Gfx g, float x, float y, float w, float h, float radius, int fill, int border) {
		round(g, x, y, w, h, radius, border);
		round(g, x + 1, y + 1, w - 2, h - 2, Math.max(0, radius - 1), fill);
	}

	// fake blur: a few transparent layers on top of each other
	public static void shadow(Gfx g, float x, float y, float w, float h, float radius, int size, float strength) {
		for (int i = size; i >= 1; i--) {
			float t = 1f - (float) i / (size + 1);
			int a = (int) (strength * 40 * t * t);
			round(g, x - i, y - i + 2, w + i * 2, h + i * 2, radius + i, a << 24);
		}
	}

	public static void circle(Gfx g, float cx, float cy, float radius, int color) {
		round(g, cx - radius, cy - radius, radius * 2, radius * 2, radius, color);
	}

	// points up at 0
	public static void arrow(Gfx g, float cx, float cy, float size, float radians, int color) {
		float s = guiScale();
		g.push();
		g.translate(cx, cy);
		g.rotate(radians);
		g.scale(1f / s);
		int len = Math.round(size * s);
		int half = len / 2;
		int head = Math.round(len * 0.55f);
		// tip, gets narrower towards the top
		for (int row = 0; row < head; row++) {
			int width = Math.max(1, Math.round((float) (row + 1) / head * len * 0.5f));
			g.fill(-width, -half + row, width, -half + row + 1, color);
		}
		int shaft = Math.max(1, Math.round(len * 0.12f));
		g.fill(-shaft, -half + head, shaft, half, color);
		g.pop();
	}

	public static int alpha(int color, float alpha) {
		int a = Math.round((color >>> 24) * Math.max(0, Math.min(1, alpha)));
		return (a << 24) | (color & 0xFFFFFF);
	}

	public static int lerp(int from, int to, float t) {
		t = Math.max(0, Math.min(1, t));
		int a = (int) ((from >>> 24) + ((to >>> 24) - (from >>> 24)) * t);
		int r = (int) (((from >> 16) & 0xFF) + (((to >> 16) & 0xFF) - ((from >> 16) & 0xFF)) * t);
		int gr = (int) (((from >> 8) & 0xFF) + (((to >> 8) & 0xFF) - ((from >> 8) & 0xFF)) * t);
		int b = (int) ((from & 0xFF) + ((to & 0xFF) - (from & 0xFF)) * t);
		return (a << 24) | (r << 16) | (gr << 8) | b;
	}

	public static String ellipsize(Font font, String text, int maxWidth) {
		if (font.width(text) <= maxWidth) return text;
		return font.plainSubstrByWidth(text, Math.max(0, maxWidth - font.width("…"))) + "…";
	}

	public static FormattedCharSequence ellipsize(Font font, Component text, int maxWidth) {
		return Component.literal(ellipsize(font, text.getString(), maxWidth)).getVisualOrderText();
	}
}
