package net.yumeverse.zeesmap.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.WorldChunk;
import net.yumeverse.zeesmap.storage.WaypointStorage;
import org.joml.Matrix4f;

import java.util.HashMap;
import java.util.Map;

public class MinimapRenderer {
    private static final int MINIMAP_SIZE = 128;
    private static final int MINIMAP_RADIUS = MINIMAP_SIZE / 2;
    private static final int RENDER_DISTANCE = 64; // blocks to render in each direction

    // Cache for map colors to improve performance
    private static final Map<BlockPos, Integer> MAP_COLOR_CACHE = new HashMap<>();
    private static long lastCacheUpdate = 0;
    private static final long CACHE_UPDATE_INTERVAL = 1000; // Update cache every second

    public static void render(DrawContext context, float tickDelta) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) return;

        int screenWidth = client.getWindow().getScaledWidth();
        int screenHeight = client.getWindow().getScaledHeight();

        // Position minimap in top-right corner
        int minimapX = screenWidth - MINIMAP_SIZE - 10;
        int minimapY = 10;

        // Clear old cache periodically
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastCacheUpdate > CACHE_UPDATE_INTERVAL) {
            MAP_COLOR_CACHE.clear();
            lastCacheUpdate = currentTime;
        }

        // Draw minimap background (black circle)
        drawCircleBackground(context, minimapX, minimapY);

        // Render the world using map-style colors
        renderMapStyleWorld(context, client, minimapX, minimapY, tickDelta);

        // Draw minimap border
        drawMinimapBorder(context, minimapX, minimapY);

        // Draw player arrow in center
        drawPlayerArrow(context, minimapX + MINIMAP_RADIUS, minimapY + MINIMAP_RADIUS, client.player.getYaw());

        // Draw waypoints
        renderWaypoints(context, client, minimapX, minimapY);

        // Draw coordinates
        drawCoordinates(context, client, minimapX, minimapY + MINIMAP_SIZE + 5);
    }

    private static void drawCircleBackground(DrawContext context, int x, int y) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        Matrix4f matrix = context.getMatrices().peek().getPositionMatrix();
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLE_FAN, VertexFormats.POSITION_COLOR);

        // Center point
        buffer.vertex(matrix, x + MINIMAP_RADIUS, y + MINIMAP_RADIUS, 0).color(0, 0, 0, 180);

        // Circle points
        for (int i = 0; i <= 32; i++) {
            float angle = (float) (i * Math.PI * 2 / 32);
            float px = x + MINIMAP_RADIUS + MathHelper.cos(angle) * MINIMAP_RADIUS;
            float py = y + MINIMAP_RADIUS + MathHelper.sin(angle) * MINIMAP_RADIUS;
            buffer.vertex(matrix, px, py, 0).color(0, 0, 0, 180);
        }

        BufferRenderer.drawWithGlobalProgram(buffer.end());
        RenderSystem.disableBlend();
    }

    private static void renderMapStyleWorld(DrawContext context, MinecraftClient client, int minimapX, int minimapY, float tickDelta) {
        World world = client.world;
        if (world == null) return;

        double playerX = client.player.getX();
        double playerZ = client.player.getZ();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        Matrix4f matrix = context.getMatrices().peek().getPositionMatrix();
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);

        // Render blocks in a grid around the player
        for (int dx = -RENDER_DISTANCE; dx < RENDER_DISTANCE; dx++) {
            for (int dz = -RENDER_DISTANCE; dz < RENDER_DISTANCE; dz++) {
                int worldX = (int) playerX + dx;
                int worldZ = (int) playerZ + dz;

                // Convert world coordinates to minimap coordinates
                float minimapPixelX = minimapX + MINIMAP_RADIUS + (dx * MINIMAP_SIZE / (RENDER_DISTANCE * 2));
                float minimapPixelY = minimapY + MINIMAP_RADIUS + (dz * MINIMAP_SIZE / (RENDER_DISTANCE * 2));

                // Check if pixel is within the circular minimap
                float distFromCenter = (float) Math.sqrt(
                        Math.pow(minimapPixelX - (minimapX + MINIMAP_RADIUS), 2) +
                                Math.pow(minimapPixelY - (minimapY + MINIMAP_RADIUS), 2)
                );

                if (distFromCenter > MINIMAP_RADIUS) continue;

                // Get map-style color for this position
                BlockPos pos = new BlockPos(worldX, 0, worldZ);
                int color = getMapStyleColor(world, pos);

                float r = ((color >> 16) & 0xFF) / 255.0f;
                float g = ((color >> 8) & 0xFF) / 255.0f;
                float b = (color & 0xFF) / 255.0f;

                float pixelSize = (float) MINIMAP_SIZE / (RENDER_DISTANCE * 2);

                // Draw a small quad for this pixel
                buffer.vertex(matrix, minimapPixelX, minimapPixelY, 0).color(r, g, b, 1.0f);
                buffer.vertex(matrix, minimapPixelX + pixelSize, minimapPixelY, 0).color(r, g, b, 1.0f);
                buffer.vertex(matrix, minimapPixelX + pixelSize, minimapPixelY + pixelSize, 0).color(r, g, b, 1.0f);
                buffer.vertex(matrix, minimapPixelX, minimapPixelY + pixelSize, 0).color(r, g, b, 1.0f);
            }
        }

        BufferRenderer.drawWithGlobalProgram(buffer.end());
        RenderSystem.disableBlend();
    }

    private static int getMapStyleColor(World world, BlockPos pos) {
        // Check cache first
        if (MAP_COLOR_CACHE.containsKey(pos)) {
            return MAP_COLOR_CACHE.get(pos);
        }

        int color = calculateMapColor(world, pos);
        MAP_COLOR_CACHE.put(pos, color);
        return color;
    }

    private static int calculateMapColor(World world, BlockPos basePos) {
        // Find the top solid block at this position
        BlockPos.Mutable mutablePos = new BlockPos.Mutable(basePos.getX(), world.getTopY(), basePos.getZ());
        BlockState topBlockState = null;
        int topY = world.getBottomY();

        // Search down to find the first non-air block
        for (int y = world.getTopY(); y >= world.getBottomY(); y--) {
            mutablePos.setY(y);
            BlockState state = world.getBlockState(mutablePos);

            if (!state.isAir() && !state.getBlock().equals(Blocks.CAVE_AIR)) {
                topBlockState = state;
                topY = y;
                break;
            }
        }

        if (topBlockState == null) {
            return 0x8E8E8E; // Default gray for void areas
        }

        // Get base color from block
        int baseColor = getBlockMapColor(topBlockState.getBlock(), world, mutablePos.setY(topY));

        // Apply height-based shading similar to Minecraft maps
        int heightDiff = topY - 64; // Sea level reference
        float shadeFactor = 1.0f + (heightDiff * 0.01f); // Subtle height shading
        shadeFactor = MathHelper.clamp(shadeFactor, 0.3f, 1.7f);

        // Apply biome tinting for certain blocks
        if (topBlockState.getBlock().equals(Blocks.GRASS_BLOCK) ||
                topBlockState.getBlock().equals(Blocks.OAK_LEAVES) ||
                topBlockState.getBlock().equals(Blocks.BIRCH_LEAVES)) {
            baseColor = applyBiomeTint(baseColor, world, mutablePos);
        }

        // Apply shading
        int r = (int) (((baseColor >> 16) & 0xFF) * shadeFactor);
        int g = (int) (((baseColor >> 8) & 0xFF) * shadeFactor);
        int b = (int) ((baseColor & 0xFF) * shadeFactor);

        r = MathHelper.clamp(r, 0, 255);
        g = MathHelper.clamp(g, 0, 255);
        b = MathHelper.clamp(b, 0, 255);

        return (r << 16) | (g << 8) | b;
    }

    private static int getBlockMapColor(Block block, World world, BlockPos pos) {
        // Water and liquids
        if (block.equals(Blocks.WATER)) return 0x4A6EF7;
        if (block.equals(Blocks.LAVA)) return 0xFF4000;
        if (block.equals(Blocks.ICE) || block.equals(Blocks.PACKED_ICE) || block.equals(Blocks.BLUE_ICE)) return 0xA0C4E4;

        // Grass and vegetation
        if (block.equals(Blocks.GRASS_BLOCK)) return 0x7CB342;
        if (block.equals(Blocks.DIRT) || block.equals(Blocks.COARSE_DIRT)) return 0x976F3A;
        if (block.equals(Blocks.PODZOL)) return 0x594A2E;
        if (block.equals(Blocks.MYCELIUM)) return 0x705D75;

        // Sand and desert
        if (block.equals(Blocks.SAND)) return 0xF7E9A3;
        if (block.equals(Blocks.RED_SAND)) return 0xD68C59;
        if (block.equals(Blocks.SANDSTONE)) return 0xF7E9A3;
        if (block.equals(Blocks.RED_SANDSTONE)) return 0xD68C59;

        // Stone and ores
        if (block.equals(Blocks.STONE) || block.equals(Blocks.COBBLESTONE)) return 0x999999;
        if (block.equals(Blocks.DEEPSLATE) || block.equals(Blocks.COBBLED_DEEPSLATE)) return 0x646464;
        if (block.equals(Blocks.GRANITE)) return 0x9F6A42;
        if (block.equals(Blocks.DIORITE)) return 0xC4C4C4;
        if (block.equals(Blocks.ANDESITE)) return 0x8A8A8A;
        if (block.equals(Blocks.BEDROCK)) return 0x565656;

        // Snow and ice
        if (block.equals(Blocks.SNOW) || block.equals(Blocks.SNOW_BLOCK) || block.equals(Blocks.POWDER_SNOW)) return 0xFFFEFE;

        // Wood and leaves
        if (block.equals(Blocks.OAK_LOG) || block.equals(Blocks.OAK_WOOD)) return 0x976F3A;
        if (block.equals(Blocks.BIRCH_LOG) || block.equals(Blocks.BIRCH_WOOD)) return 0xD7CA8B;
        if (block.equals(Blocks.SPRUCE_LOG) || block.equals(Blocks.SPRUCE_WOOD)) return 0x6B4423;
        if (block.equals(Blocks.JUNGLE_LOG) || block.equals(Blocks.JUNGLE_WOOD)) return 0x976F3A;
        if (block.equals(Blocks.ACACIA_LOG) || block.equals(Blocks.ACACIA_WOOD)) return 0xBA7E53;
        if (block.equals(Blocks.DARK_OAK_LOG) || block.equals(Blocks.DARK_OAK_WOOD)) return 0x4A2F17;
        if (block.equals(Blocks.CHERRY_LOG) || block.equals(Blocks.CHERRY_WOOD)) return 0xE8B4CB;
        if (block.equals(Blocks.MANGROVE_LOG) || block.equals(Blocks.MANGROVE_WOOD)) return 0x7A5543;

        // Leaves
        if (block.equals(Blocks.OAK_LEAVES)) return 0x59AE30;
        if (block.equals(Blocks.BIRCH_LEAVES)) return 0x8DB360;
        if (block.equals(Blocks.SPRUCE_LEAVES)) return 0x619A3C;
        if (block.equals(Blocks.JUNGLE_LEAVES)) return 0x30B95A;
        if (block.equals(Blocks.ACACIA_LEAVES)) return 0x9CAB3C;
        if (block.equals(Blocks.DARK_OAK_LEAVES)) return 0x2D5016;
        if (block.equals(Blocks.CHERRY_LEAVES)) return 0xF2B2D6;
        if (block.equals(Blocks.MANGROVE_LEAVES)) return 0x59AE30;

        // Nether blocks
        if (block.equals(Blocks.NETHERRACK)) return 0x7A342A;
        if (block.equals(Blocks.NETHER_BRICKS)) return 0x2C1414;
        if (block.equals(Blocks.SOUL_SAND) || block.equals(Blocks.SOUL_SOIL)) return 0x4C3426;
        if (block.equals(Blocks.CRIMSON_NYLIUM)) return 0x943F61;
        if (block.equals(Blocks.WARPED_NYLIUM)) return 0x167E86;

        // End blocks
        if (block.equals(Blocks.END_STONE)) return 0xE0D99A;
        if (block.equals(Blocks.PURPUR_BLOCK)) return 0xAB8AAB;

        // Ores (more vibrant colors for visibility)
        if (block.equals(Blocks.COAL_ORE) || block.equals(Blocks.DEEPSLATE_COAL_ORE)) return 0x343434;
        if (block.equals(Blocks.IRON_ORE) || block.equals(Blocks.DEEPSLATE_IRON_ORE)) return 0xD8AF93;
        if (block.equals(Blocks.GOLD_ORE) || block.equals(Blocks.DEEPSLATE_GOLD_ORE)) return 0xFCEE4B;
        if (block.equals(Blocks.DIAMOND_ORE) || block.equals(Blocks.DEEPSLATE_DIAMOND_ORE)) return 0x5CDBD5;
        if (block.equals(Blocks.EMERALD_ORE) || block.equals(Blocks.DEEPSLATE_EMERALD_ORE)) return 0x00D93A;
        if (block.equals(Blocks.REDSTONE_ORE) || block.equals(Blocks.DEEPSLATE_REDSTONE_ORE)) return 0xD93A00;
        if (block.equals(Blocks.LAPIS_ORE) || block.equals(Blocks.DEEPSLATE_LAPIS_ORE)) return 0x4A4AFF;
        if (block.equals(Blocks.COPPER_ORE) || block.equals(Blocks.DEEPSLATE_COPPER_ORE)) return 0xFF6A00;

        // Clay
        if (block.equals(Blocks.CLAY)) return 0xA3A3A3;

        // Concrete and terracotta (various colors)
        if (block.equals(Blocks.WHITE_CONCRETE) || block.equals(Blocks.WHITE_TERRACOTTA)) return 0xD5D5D5;
        if (block.equals(Blocks.BLACK_CONCRETE) || block.equals(Blocks.BLACK_TERRACOTTA)) return 0x1D1D1D;
        if (block.equals(Blocks.RED_CONCRETE) || block.equals(Blocks.RED_TERRACOTTA)) return 0xB02E26;
        if (block.equals(Blocks.GREEN_CONCRETE) || block.equals(Blocks.GREEN_TERRACOTTA)) return 0x5E7C16;
        if (block.equals(Blocks.BLUE_CONCRETE) || block.equals(Blocks.BLUE_TERRACOTTA)) return 0x3C44AA;
        if (block.equals(Blocks.YELLOW_CONCRETE) || block.equals(Blocks.YELLOW_TERRACOTTA)) return 0xF9D71C;

        // Default color for unknown blocks
        return 0x8E8E8E;
    }

    private static int applyBiomeTint(int baseColor, World world, BlockPos pos) {
        try {
            Biome biome = world.getBiome(pos).value();

            // Get biome temperature - this should work in 1.21.1
            float temperature = biome.getTemperature();

            // For humidity/downfall, we'll use a simplified approach since the API may have changed
            // We'll estimate humidity based on biome type and temperature
            float humidity = estimateBiomeHumidity(biome, temperature);

            // Adjust color based on temperature and humidity
            float tempFactor = MathHelper.clamp(temperature, 0.0f, 1.0f);
            float humidityFactor = MathHelper.clamp(humidity, 0.0f, 1.0f);

            int r = (baseColor >> 16) & 0xFF;
            int g = (baseColor >> 8) & 0xFF;
            int b = baseColor & 0xFF;

            // Cool biomes tend to be more blue-green
            if (temperature < 0.3f) {
                g = (int) (g * 0.9f);
                b = (int) (b * 1.1f);
            }
            // Hot biomes tend to be more yellow-brown
            else if (temperature > 0.8f) {
                r = (int) (r * 1.1f);
                g = (int) (g * 0.95f);
                b = (int) (b * 0.8f);
            }

            // Dry biomes are less vibrant
            if (humidity < 0.3f) {
                r = (int) (r * 0.9f);
                g = (int) (g * 0.85f);
                b = (int) (b * 0.8f);
            }

            r = MathHelper.clamp(r, 0, 255);
            g = MathHelper.clamp(g, 0, 255);
            b = MathHelper.clamp(b, 0, 255);

            return (r << 16) | (g << 8) | b;
        } catch (Exception e) {
            return baseColor; // Return original color if biome data is unavailable
        }
    }

    // Helper method to estimate humidity since getDownfall() might not be available
    private static float estimateBiomeHumidity(Biome biome, float temperature) {
        // This is a simplified estimation based on common biome characteristics
        // In a real implementation, you might want to use biome-specific data

        // Very cold biomes tend to be dry (except for some exceptions)
        if (temperature < 0.2f) {
            return 0.2f;
        }
        // Desert-like temperatures tend to be dry
        else if (temperature > 1.0f) {
            return 0.1f;
        }
        // Temperate biomes have moderate humidity
        else if (temperature > 0.5f && temperature < 0.8f) {
            return 0.6f;
        }
        // Default moderate humidity
        else {
            return 0.4f;
        }
    }

    private static void drawMinimapBorder(DrawContext context, int x, int y) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        Matrix4f matrix = context.getMatrices().peek().getPositionMatrix();
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);

        // Draw circle border
        for (int i = 0; i <= 64; i++) {
            float angle = (float) (i * Math.PI * 2 / 64);
            float innerRadius = MINIMAP_RADIUS - 1;
            float outerRadius = MINIMAP_RADIUS + 1;

            float innerX = x + MINIMAP_RADIUS + MathHelper.cos(angle) * innerRadius;
            float innerY = y + MINIMAP_RADIUS + MathHelper.sin(angle) * innerRadius;
            float outerX = x + MINIMAP_RADIUS + MathHelper.cos(angle) * outerRadius;
            float outerY = y + MINIMAP_RADIUS + MathHelper.sin(angle) * outerRadius;

            buffer.vertex(matrix, innerX, innerY, 0).color(1.0f, 1.0f, 1.0f, 1.0f);
            buffer.vertex(matrix, outerX, outerY, 0).color(1.0f, 1.0f, 1.0f, 1.0f);
        }

        BufferRenderer.drawWithGlobalProgram(buffer.end());
        RenderSystem.disableBlend();
    }

    private static void drawPlayerArrow(DrawContext context, int centerX, int centerY, float yaw) {
        context.getMatrices().push();
        context.getMatrices().translate(centerX, centerY, 0);
        context.getMatrices().multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Z.rotationDegrees(yaw + 90));

        // Draw a simple arrow pointing up (north)
        Matrix4f matrix = context.getMatrices().peek().getPositionMatrix();
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);

        // Arrow triangle
        buffer.vertex(matrix, 0, -6, 0).color(1.0f, 1.0f, 1.0f, 1.0f);
        buffer.vertex(matrix, -4, 4, 0).color(1.0f, 1.0f, 1.0f, 1.0f);
        buffer.vertex(matrix, 4, 4, 0).color(1.0f, 1.0f, 1.0f, 1.0f);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        BufferRenderer.drawWithGlobalProgram(buffer.end());
        RenderSystem.disableBlend();

        context.getMatrices().pop();
    }

    private static void renderWaypoints(DrawContext context, MinecraftClient client, int minimapX, int minimapY) {
        if (client.player == null) return;

        double playerX = client.player.getX();
        double playerZ = client.player.getZ();

        for (WaypointStorage.Waypoint waypoint : WaypointStorage.waypoints) {
            double deltaX = waypoint.x - playerX;
            double deltaZ = waypoint.z - playerZ;

            // Convert to minimap coordinates
            float minimapDeltaX = (float) (deltaX * MINIMAP_SIZE / (RENDER_DISTANCE * 2));
            float minimapDeltaZ = (float) (deltaZ * MINIMAP_SIZE / (RENDER_DISTANCE * 2));

            int waypointX = (int) (minimapX + MINIMAP_RADIUS + minimapDeltaX);
            int waypointY = (int) (minimapY + MINIMAP_RADIUS + minimapDeltaZ);

            // Check if waypoint is within the circular minimap
            float distFromCenter = (float) Math.sqrt(
                    Math.pow(minimapDeltaX, 2) + Math.pow(minimapDeltaZ, 2)
            );

            if (distFromCenter <= MINIMAP_RADIUS - 4) {
                // Draw waypoint marker
                drawWaypointMarker(context, waypointX, waypointY, 0xFF0000); // Red color
            }
        }
    }

    private static void drawWaypointMarker(DrawContext context, int x, int y, int color) {
        float r = ((color >> 16) & 0xFF) / 255.0f;
        float g = ((color >> 8) & 0xFF) / 255.0f;
        float b = (color & 0xFF) / 255.0f;

        Matrix4f matrix = context.getMatrices().peek().getPositionMatrix();
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);

        // Draw a small square for the waypoint
        int size = 3;
        buffer.vertex(matrix, x - size, y - size, 0).color(r, g, b, 1.0f);
        buffer.vertex(matrix, x + size, y - size, 0).color(r, g, b, 1.0f);
        buffer.vertex(matrix, x + size, y + size, 0).color(r, g, b, 1.0f);
        buffer.vertex(matrix, x - size, y + size, 0).color(r, g, b, 1.0f);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        BufferRenderer.drawWithGlobalProgram(buffer.end());
        RenderSystem.disableBlend();
    }

    private static void drawCoordinates(DrawContext context, MinecraftClient client, int x, int y) {
        if (client.player == null) return;

        String coords = String.format("XYZ: %d, %d, %d",
                (int) client.player.getX(),
                (int) client.player.getY(),
                (int) client.player.getZ());

        context.drawText(client.textRenderer, coords, x, y, 0xFFFFFF, true);
    }
}