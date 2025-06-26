package net.yumeverse.zeesmap.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.math.MatrixStack;
import net.yumeverse.zeesmap.storage.WaypointStorage;

public class MinimapRenderer {
    private static final int SIZE = 100;

    public static void render(MatrixStack ms) {
        var client = MinecraftClient.getInstance();
        var player = client.player;
        if (player == null) return;

        double px = player.getX(), pz = player.getZ();

        var tex = client.getTextureManager()
                .getTextureId("zeesmap/minimap_bg");
        RenderSystem.setShaderTexture(0, tex);
        RenderSystem.drawTexture(ms, 5, 5, 0, 0, SIZE, SIZE, SIZE, SIZE);

        var icon = client.getTextureManager()
                .getTextureId("zeesmap/waypoint_icon");
        RenderSystem.setShaderTexture(0, icon);

        for (var wp : WaypointStorage.waypoints) {
            int dx = (int)((wp.x - px) * 2) + 5 + SIZE/2;
            int dz = (int)((wp.z - pz) * 2) + 5 + SIZE/2;
            RenderSystem.drawTexture(ms, dx - 4, dz - 4, 0, 0, 8, 8, 8, 8);
        }
    }
}
