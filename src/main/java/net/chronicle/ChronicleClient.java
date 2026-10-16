package net.chronicle;

import com.mojang.blaze3d.platform.InputConstants;
import net.chronicle.compat.Platform;
import net.chronicle.config.ChronicleConfig;
import net.chronicle.hud.Hud;
import net.chronicle.journal.EventTracker;
import net.chronicle.journal.JournalManager;
import net.chronicle.ui.JournalScreens;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// client only, the server never knows about any of this
public final class ChronicleClient implements ClientModInitializer {
	public static final String MOD_ID = "chronicle";
	public static final Logger LOG = LoggerFactory.getLogger("Chronicle");

	private static KeyMapping openKey;
	private static KeyMapping noteKey;

	@Override
	public void onInitializeClient() {
		ChronicleConfig.load();
		openKey = Platform.registerKey("key.chronicle.open", InputConstants.KEY_J);
		noteKey = Platform.registerKey("key.chronicle.quick_note", InputConstants.UNKNOWN.getValue());

		ClientTickEvents.END_CLIENT_TICK.register(ChronicleClient::tick);
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> JournalManager.unload());
		ClientLifecycleEvents.CLIENT_STOPPING.register(client -> JournalManager.save());
		ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
			if (!overlay) EventTracker.onGameMessage(message);
		});
		Platform.registerHud(Hud::render);
	}

	private static void tick(Minecraft mc) {
		JournalManager.tick(mc);
		EventTracker.tick(mc);
		Hud.tick();
		while (openKey.consumeClick()) {
			JournalScreens.open();
		}
		while (noteKey.consumeClick()) {
			JournalScreens.openNewNote();
		}
	}
}
