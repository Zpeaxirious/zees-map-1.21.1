package net.yumeverse.zeesmap.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.yumeverse.zeesmap.storage.WaypointStorage;

public class WaypointManagerScreen extends Screen {
    protected WaypointManagerScreen() {
        super(Text.of("Zee Map Waypoints"));
    }

    @Override
    protected void init() {
        int y = 20;
        for (var wp : WaypointStorage.waypoints) {
            this.addDrawableChild(new ButtonWidget(10, y, 150, 20, Text.of(wp.name), btn -> {
                MinecraftClient.getInstance().player
                        .requestTeleport(wp.x, wp.y, wp.z);
            }));
            y += 24;
        }
        this.addDrawableChild(new ButtonWidget(10, y, 150, 20, Text.of("Add Waypoint"), btn -> {
            MinecraftClient.getInstance().setScreen(
                    new AddWaypointScreen(this));
        }));
    }
}
