package net.chronicle.compat;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.toasts.ToastManager;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

public final class Platform {
	private static final KeyMapping.Category KEY_CATEGORY = KeyMapping.Category.register(ResourceLocation.fromNamespaceAndPath("chronicle", "main"));

	private Platform() {
	}

	public static KeyMapping registerKey(String name, int defaultKey) {
		return KeyBindingHelper.registerKeyBinding(new KeyMapping(name, defaultKey, KEY_CATEGORY));
	}

	public static void registerHud(Consumer<Gfx> renderer) {
		HudElementRegistry.addLast(ResourceLocation.fromNamespaceAndPath("chronicle", "hud"), (g, delta) -> renderer.accept(new Gfx(g)));
	}

	public static String dimensionId(Level level) {
		return level.dimension().location().toString();
	}

	public static Screen screen() {
		return Minecraft.getInstance().screen;
	}

	public static void setScreen(Screen screen) {
		Minecraft.getInstance().setScreen(screen);
	}

	public static ToastManager toasts() {
		return Minecraft.getInstance().getToastManager();
	}

	public static boolean hudHidden() {
		return Minecraft.getInstance().options.hideGui;
	}

	public static <T> CycleButton<T> cycle(Function<T, Component> names, List<T> values, T initial, int x, int y, int width, int height,
			Component label, CycleButton.OnValueChange<T> onChange) {
		return CycleButton.builder(names).withValues(values).withInitialValue(initial).create(x, y, width, height, label, onChange);
	}
}
