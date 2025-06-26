package net.yumeverse.zeesmap.gui;

import net.minecraft.client.MinecraftClient;
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
        int y = 20;
        nameField = new TextFieldWidget(textRenderer, 10, y, 200, 20, Text.of("Name")); y+=24;
        xField = new TextFieldWidget(textRenderer, 10, y, 200, 20, Text.of("X")); y+=24;
        yField = new TextFieldWidget(textRenderer, 10, y, 200, 20, Text.of("Y")); y+=24;
        zField = new TextFieldWidget(textRenderer, 10, y, 200, 20, Text.of("Z")); y+=24;
        addSelectableChild(nameField);
        addSelectableChild(xField);
        addSelectableChild(yField);
        addSelectableChild(zField);

        addDrawableChild(new ButtonWidget(10, y, 100, 20, Text.of("Save"), btn -> {
            try {
                WaypointStorage.waypoints.add(new WaypointStorage.Waypoint(
                        nameField.getText(),
                        Double.parseDouble(xField.getText()),
                        Double.parseDouble(yField.getText()),
                        Double.parseDouble(zField.getText())
                ));
                WaypointStorage.save();
                MinecraftClient.getInstance().setScreen(parent);
            } catch (NumberFormatException e) {
                // optionally show error
            }
        }));
        addDrawableChild(new ButtonWidget(120, y, 100, 20, Text.of("Cancel"), btn -> {
            MinecraftClient.getInstance().setScreen(parent);
        }));
    }
}
