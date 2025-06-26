package net.yumeverse.zeesmap.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.yumeverse.zeesmap.storage.WaypointStorage;

public class WaypointManagerScreen extends Screen {
    public WaypointManagerScreen() {
        super(Text.of("Zee's Map Waypoints"));
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int y = 40;

        for (int i = 0; i < WaypointStorage.waypoints.size(); i++) {
            var wp = WaypointStorage.waypoints.get(i);
            final int index = i;

            // Waypoint button
            addDrawableChild(ButtonWidget.builder(
                    Text.of(wp.name + " (" + (int)wp.x + ", " + (int)wp.y + ", " + (int)wp.z + ")"),
                    btn -> {
                        // Teleport (creative mode only)
                        if (client != null && client.player != null && client.player.getAbilities().creativeMode) {
                            client.player.requestTeleport(wp.x, wp.y, wp.z);
                            this.close();
                        }
                    }
            ).dimensions(centerX - 150, y, 200, 20).build());

            // Delete button
            addDrawableChild(ButtonWidget.builder(Text.of("X"), btn -> {
                WaypointStorage.waypoints.remove(index);
                WaypointStorage.save();
                this.clearAndInit(); // Refresh the screen
            }).dimensions(centerX + 60, y, 20, 20).build());

            y += 25;
        }

        // Add waypoint button
        addDrawableChild(ButtonWidget.builder(Text.of("Add Waypoint"), btn -> {
            MinecraftClient.getInstance().setScreen(new AddWaypointScreen(this));
        }).dimensions(centerX - 75, y + 10, 150, 20).build());

        // Close button
        addDrawableChild(ButtonWidget.builder(Text.of("Close"), btn -> {
            this.close();
        }).dimensions(centerX - 50, y + 40, 100, 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(textRenderer, this.title, this.width / 2, 15, 0xFFFFFF);
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }
}