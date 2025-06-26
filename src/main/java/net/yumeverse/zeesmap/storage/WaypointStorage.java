package net.yumeverse.zeesmap.storage;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.minecraft.client.MinecraftClient;

import java.io.*;
import java.nio.file.*;
import java.util.*;

public class WaypointStorage {
    public static List<Waypoint> waypoints = new ArrayList<>();
    private static final Path path = MinecraftClient.getInstance()
            .runDirectory.toPath()
            .resolve("config/zeesmap_waypoints.json");
    private static final Gson gson = new Gson();

    public static void load() {
        try {
            if (Files.exists(path)) {
                try (Reader r = Files.newBufferedReader(path)) {
                    waypoints = gson.fromJson(r, new TypeToken<List<Waypoint>>(){}.getType());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void save() {
        try {
            Files.createDirectories(path.getParent());
            try (Writer w = Files.newBufferedWriter(path)) {
                gson.toJson(waypoints, w);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static class Waypoint {
        public String name;
        public double x, y, z;
        public Waypoint(String name, double x, double y, double z) {
            this.name = name;
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }
}
