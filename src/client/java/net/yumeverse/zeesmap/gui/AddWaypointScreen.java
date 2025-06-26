package net.yumeverse.zeesmap.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.yumeverse.zeesmap.storage.WaypointStorage;

public class AddWaypointScreen extends Screen {
    private final Screen parent;
    private TextFieldWidget nameField, xField, yField, zField;

    public AddWaypointScreen(Screen parent) {
        super(Text.of("Add Waypoint"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int y = 60;

        nameField = new TextFieldWidget(textRenderer, centerX - 100, y, 200, 20, Text.of("Name"));
        nameField.setPlaceholder(Text.of("Waypoint Name"));
        y += 30;

        xField = new TextFieldWidget(textRenderer, centerX - 100, y, 200, 20, Text.of("X"));
        xField.setPlaceholder(Text.of("X Coordinate"));
        if (client != null && client.player != null) {
            xField.setText(String.valueOf((int)client.player.getX()));
        }
        y += 30;

        yField = new TextFieldWidget(textRenderer, centerX - 100, y, 200, 20, Text.of("Y"));
        yField.setPlaceholder(Text.of("Y Coordinate"));
        if (client != null && client.player != null) {
            yField.setText(String.valueOf((int)client.player.getY()));
        }
        y += 30;

        zField = new TextFieldWidget(textRenderer, centerX - 100, y, 200, 20, Text.of("Z"));
        zField.setPlaceholder(Text.of("Z Coordinate"));
        if (client != null && client.player != null) {
            zField.setText(String.valueOf((int)client.player.getZ()));
        }
        y += 40;

        addSelectableChild(nameField);
        addSelectableChild(xField);
        addSelectableChild(yField);
        addSelectableChild(zField);

        addDrawableChild(ButtonWidget.builder(Text.of("Save"), btn -> {
            try {
                String name = nameField.getText().trim();
                if (name.isEmpty()) name = "Waypoint";

                WaypointStorage.waypoints.add(new WaypointStorage.Waypoint(
                        name,
                        Double.parseDouble(xField.getText()),
                        Double.parseDouble(yField.getText()),
                        Double.parseDouble(zField.getText())
                ));
                WaypointStorage.save();
                MinecraftClient.getInstance().setScreen(parent);
            } catch (NumberFormatException e) {
                // Show error message or handle gracefully
            }
        }).dimensions(centerX - 60, y, 50, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.of("Cancel"), btn -> {
            MinecraftClient.getInstance().setScreen(parent);
        }).dimensions(centerX + 10, y, 50, 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(textRenderer, this.title, this.width / 2, 20, 0xFFFFFF);

        // Draw field labels
        context.drawText(textRenderer, "Name:", nameField.getX(), nameField.getY() - 12, 0xFFFFFF, false);
        context.drawText(textRenderer, "X:", xField.getX(), xField.getY() - 12, 0xFFFFFF, false);
        context.drawText(textRenderer, "Y:", yField.getX(), yField.getY() - 12, 0xFFFFFF, false);
        context.drawText(textRenderer, "Z:", zField.getX(), zField.getY() - 12, 0xFFFFFF, false);

        super.render(context, mouseX, mouseY, delta);
    }
}