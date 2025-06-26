package net.yumeverse.zeesmap;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;

public class ZeeMapMod implements ClientModInitializer {
	public static KeyBinding OPEN_WAYPOINT_SCREEN;

	@Override
	public void onInitializeClient() {
		WaypointStorage.load();

		OPEN_WAYPOINT_SCREEN = KeyBindingHelper.registerKeyBinding(new KeyBinding(
				"key.zeesmap.open",
				InputUtil.Type.KEYSYM,
				InputUtil.GLFW_KEY_M,
				"category.zeesmap"
		));

		HudRenderCallback.EVENT.register((matrix, delta) -> {
			render.MinimapRenderer.render(matrix);
		});

		KeyBindingHelper.watch(keys -> {
			if (OPEN_WAYPOINT_SCREEN.wasPressed()) {
				net.minecraft.client.MinecraftClient.getInstance()
						.setScreen(new gui.WaypointManagerScreen());
			}
		});
	}
}
