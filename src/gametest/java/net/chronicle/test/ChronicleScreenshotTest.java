package net.chronicle.test;

import net.chronicle.config.ChronicleConfig;
import net.chronicle.hud.Notifier;
import net.chronicle.journal.Category;
import net.chronicle.journal.JournalEntry;
import net.chronicle.journal.JournalManager;
import net.chronicle.ui.modern.ModernJournalScreen;
import net.chronicle.ui.vanilla.VanillaJournalScreen;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

// ./gradlew :<version>:runClientGameTest, images go to versions/<version>/build/run/clientGameTest/screenshots
public class ChronicleScreenshotTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext ctx) {
		ctx.getInput().resizeWindow(1280, 720);
		ctx.runOnClient(mc -> mc.options.guiScale().set(2));
		ctx.getInput().resizeWindow(1280, 721);
		ctx.getInput().resizeWindow(1280, 720);

		try (TestSingleplayerContext world = ctx.worldBuilder().create()) {
			ctx.waitFor(mc -> JournalManager.get() != null && JournalManager.get().started);
			ctx.waitTicks(40);
			ctx.runOnClient(mc -> fillSampleData(mc));

			// modern
			ctx.runOnClient(mc -> {
				ChronicleConfig.get().style = ChronicleConfig.UiStyle.MODERN;
				Notifier.show(Component.translatable("chronicle.toast.item"), Component.literal("Erster Fund: Diamant"),
						JournalManager.get().entries.get(JournalManager.get().entries.size() - 4));
			});
			ctx.waitTicks(15);
			ctx.takeScreenshot("chronicle-hud-modern");

			ctx.setScreen(() -> new ModernJournalScreen(null));
			ctx.waitTicks(20);
			ctx.takeScreenshot("chronicle-modern-list");

			clickGui(ctx, layoutListX(ctx) + 60, layoutListY(ctx) + 12);
			ctx.waitTicks(20);
			ctx.takeScreenshot("chronicle-modern-expanded");

			clickGui(ctx, layoutGearX(ctx), layoutHeaderY(ctx));
			ctx.waitTicks(20);
			ctx.takeScreenshot("chronicle-modern-settings");

			ctx.setScreen(() -> ModernJournalScreen.withNewNote(null));
			ctx.waitTicks(10);
			ctx.getInput().typeChars("Geheime Höhle");
			ctx.waitTicks(20);
			ctx.takeScreenshot("chronicle-modern-note");

			// vanilla
			ctx.setScreen(() -> null);
			ctx.runOnClient(mc -> ChronicleConfig.get().style = ChronicleConfig.UiStyle.VANILLA);
			ctx.runOnClient(mc -> Notifier.show(Component.translatable("chronicle.toast.biome"), Component.literal("Neues Biom: Kirschgarten"), null));
			ctx.waitTicks(15);
			ctx.takeScreenshot("chronicle-hud-vanilla");

			ctx.setScreen(() -> new VanillaJournalScreen(null));
			ctx.waitTicks(10);
			int gw = ctx.computeOnClient(mc -> mc.getWindow().getGuiScaledWidth());
			clickGui(ctx, gw / 2, 48 + 13);
			ctx.waitTicks(10);
			ctx.takeScreenshot("chronicle-vanilla-list");

			ctx.clickScreenButton("chronicle.button.details");
			ctx.waitTicks(10);
			ctx.takeScreenshot("chronicle-vanilla-details");

			ctx.setScreen(() -> new VanillaJournalScreen(null));
			ctx.waitTicks(5);
			ctx.clickScreenButton("chronicle.button.settings");
			ctx.waitTicks(10);
			ctx.takeScreenshot("chronicle-vanilla-settings");

			ctx.setScreen(() -> new VanillaJournalScreen(null));
			ctx.waitTicks(5);
			ctx.clickScreenButton("chronicle.button.new_note");
			ctx.waitTicks(5);
			ctx.getInput().typeChars("Geheime Höhle");
			ctx.waitTicks(10);
			ctx.takeScreenshot("chronicle-vanilla-note");
			ctx.setScreen(() -> null);

			// gui scale 4 -> compact layout
			ctx.runOnClient(mc -> {
				mc.options.guiScale().set(4);
				ChronicleConfig.get().style = ChronicleConfig.UiStyle.MODERN;
			});
			ctx.getInput().resizeWindow(1280, 721);
			ctx.getInput().resizeWindow(1280, 720);
			ctx.setScreen(() -> new ModernJournalScreen(null));
			ctx.waitTicks(20);
			ctx.takeScreenshot("chronicle-modern-compact");
			ctx.setScreen(() -> null);
		}
	}

	private static void fillSampleData(Minecraft mc) {
		var p = mc.player;
		long now = System.currentTimeMillis();
		int x = p.getBlockX(), y = p.getBlockY(), z = p.getBlockZ();
		add(JournalEntry.of(Category.BIOME, "biome").subjectKey("biome.minecraft.cherry_grove", "Cherry Grove")
				.icon("minecraft:cherry_sapling").at("minecraft:overworld", x + 340, 92, z - 120), now - 3 * 3600_000L);
		add(JournalEntry.of(Category.DIMENSION, "dimension").subjectKey("chronicle.dimension.minecraft.the_nether", "The Nether")
				.icon("minecraft:netherrack").at("minecraft:the_nether", x / 8 + 12, 70, z / 8 - 4), now - 2 * 3600_000L);
		add(JournalEntry.of(Category.DEATH, "death").subject("1")
				.text("Steve wurde von einem Creeper in die Luft gesprengt\nLevel 7 · 23 min überlebt").at("minecraft:overworld", x - 80, 41, z + 230), now - 95 * 60_000L);
		add(JournalEntry.of(Category.ITEM, "item").subjectKey("item.minecraft.diamond", "Diamond")
				.icon("minecraft:diamond").at("minecraft:overworld", x - 60, -54, z + 200), now - 50 * 60_000L);
		add(JournalEntry.of(Category.MILESTONE, "level").subject("30").icon("minecraft:experience_bottle")
				.at("minecraft:overworld", x + 10, 64, z + 5), now - 20 * 60_000L);
		JournalEntry village = JournalEntry.of(Category.NOTE, "note").subject("Dorf mit Schmied")
				.text("Zwei Eisengolems. Der Werkzeugschmied verkauft eine Diamantspitzhacke.").at("minecraft:overworld", x + 150, 70, z - 40);
		village.pinned = true;
		add(village, now - 8 * 60_000L);
		add(JournalEntry.of(Category.NOTE, "note").subject("Zombie-Spawner").text("Unter dem Fluss, gut beleuchten!")
				.at("minecraft:overworld", x - 25, -23, z + 60), now - 3 * 60_000L);
		JournalManager.setTarget(village);
	}

	private static void add(JournalEntry entry, long time) {
		entry.time = time;
		JournalManager.add(entry, false);
	}

	// same math as ModernJournalScreen#init so we can click specific things.
	// if the layout changes this has to change too
	private static int[] panel(ClientGameTestContext ctx) {
		int w = ctx.computeOnClient(mc -> mc.getWindow().getGuiScaledWidth());
		int h = ctx.computeOnClient(mc -> mc.getWindow().getGuiScaledHeight());
		int pw = Math.min(w - 16, 700), ph = Math.min(h - 16, 420);
		return new int[] {(w - pw) / 2, (h - ph) / 2, pw, ph};
	}

	private static int layoutListX(ClientGameTestContext ctx) {
		int[] p = panel(ctx);
		int sidebar = p[2] >= 440 ? 124 : 28;
		return p[0] + 8 + sidebar + 10;
	}

	private static int layoutListY(ClientGameTestContext ctx) {
		return panel(ctx)[1] + 62;
	}

	private static int layoutGearX(ClientGameTestContext ctx) {
		int[] p = panel(ctx);
		return p[0] + p[2] - 8 - 18 - 22 + 9;
	}

	private static int layoutHeaderY(ClientGameTestContext ctx) {
		return panel(ctx)[1] + 16;
	}

	private static void clickGui(ClientGameTestContext ctx, int guiX, int guiY) {
		double scale = ctx.computeOnClient(mc -> (double) mc.getWindow().getGuiScale());
		ctx.getInput().setCursorPos(guiX * scale, guiY * scale);
		ctx.waitTick();
		ctx.getInput().pressMouse(0);
	}
}
