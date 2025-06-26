package net.yumeverse.zeesmap.storage;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;

import java.io.*;
import java.nio.file.*;
import java.util.*;

public class WaypointStorage {
    public static List<Waypoint> waypoints = new ArrayList<>();
    private static Path path;
    private static final Gson gson = new Gson();

    // Initialize path when first accessed
    private static Path getPath() {
        if (path == null) {
            path = FabricLoader.getInstance()
                    .getConfigDir()
                    .resolve("zeesmap_waypoints.json");
        }
        return path;
    }

    public static void load() {
        try {
            Path configPath = getPath();
            if (Files.exists(configPath)) {
                try (Reader r = Files.newBufferedReader(configPath)) {
                    List<Waypoint> loaded = gson.fromJson(r, new TypeToken<List<Waypoint>>(){}.getType());
                    if (loaded != null) {
                        waypoints = loaded;
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Failed to load waypoints: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void save() {
        try {
            Path configPath = getPath();
            Files.createDirectories(configPath.getParent());
            try (Writer w = Files.newBufferedWriter(configPath)) {
                gson.toJson(waypoints, w);
            }
        } catch (IOException e) {
            System.err.println("Failed to save waypoints: " + e.getMessage());
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

        // Default constructor for Gson
        public Waypoint() {}
    }
}