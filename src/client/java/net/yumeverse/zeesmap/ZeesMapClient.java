package net.yumeverse.zeesmap;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.MinecraftClient;
import net.yumeverse.zeesmap.render.MinimapRenderer;
import net.yumeverse.zeesmap.gui.WaypointManagerScreen;
import net.yumeverse.zeesmap.storage.WaypointStorage;

public class ZeesMapClient implements ClientModInitializer {
	public static KeyBinding OPEN_WAYPOINT_SCREEN;

	@Override
	public void onInitializeClient() {
		// Initialize waypoint storage (client-only)
		WaypointStorage.load();

		OPEN_WAYPOINT_SCREEN = KeyBindingHelper.registerKeyBinding(new KeyBinding(
				"key.zeesmap.open",
				InputUtil.Type.KEYSYM,
				InputUtil.GLFW_KEY_M,
				"category.zeesmap"
		));

		HudRenderCallback.EVENT.register((drawContext, renderTickCounter) -> {
			MinimapRenderer.render(drawContext, renderTickCounter.getTickDelta(true));
		});

		// Check for key press each tick
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (OPEN_WAYPOINT_SCREEN.wasPressed()) {
				MinecraftClient.getInstance().setScreen(new WaypointManagerScreen());
			}
		});
	}
}