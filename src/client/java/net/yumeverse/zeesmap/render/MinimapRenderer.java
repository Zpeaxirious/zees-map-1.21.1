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
    private static final int SAMPLE_RATE = 2; // Sample every 2 blocks for better performance

    // Cache for map colors to improve performance
    private static final Map<BlockPos, Integer> MAP_COLOR_CACHE = new HashMap<>();
    private static long lastCacheUpdate = 0;
    private static final long CACHE_UPDATE_INTERVAL = 2000; // Update cache every 2 seconds

    public static void render(DrawContext context, float tickDelta) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) return;

        // Ensure we're in a valid rendering state
        if (client.isPaused() || client.options.hudHidden) return;

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

        // Save current GL state
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();

        // Push matrix state
        context.getMatrices().push();

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

        // Restore matrix state
        context.getMatrices().pop();

        // Restore GL state
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }

    private static void drawCircleBackground(DrawContext context, int x, int y) {
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
    }

    private static void renderMapStyleWorld(DrawContext context, MinecraftClient client, int minimapX, int minimapY, float tickDelta) {
        World world = client.world;
        if (world == null) return;

        double playerX = client.player.getX();
        double playerZ = client.player.getZ();

        Matrix4f matrix = context.getMatrices().peek().getPositionMatrix();
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);

        // Calculate the pixel size for each block
        float pixelSize = (float) MINIMAP_SIZE / (RENDER_DISTANCE * 2);

        // Render blocks in a grid around the player
        for (int dx = -RENDER_DISTANCE; dx < RENDER_DISTANCE; dx += SAMPLE_RATE) {
            for (int dz = -RENDER_DISTANCE; dz < RENDER_DISTANCE; dz += SAMPLE_RATE) {
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

                // Get map-style color for this position - ensure we have a valid chunk
                BlockPos pos = new BlockPos(worldX, 0, worldZ);

                // Check if chunk is loaded before trying to access it
                if (!world.isChunkLoaded(worldX >> 4, worldZ >> 4)) {
                    continue; // Skip if chunk isn't loaded
                }

                int color = getMapStyleColor(world, pos);

                float r = ((color >> 16) & 0xFF) / 255.0f;
                float g = ((color >> 8) & 0xFF) / 255.0f;
                float b = (color & 0xFF) / 255.0f;

                // Make the pixel size slightly larger to cover gaps
                float renderPixelSize = pixelSize * SAMPLE_RATE;

                // Draw a quad for this pixel
                buffer.vertex(matrix, minimapPixelX, minimapPixelY, 0).color(r, g, b, 1.0f);
                buffer.vertex(matrix, minimapPixelX + renderPixelSize, minimapPixelY, 0).color(r, g, b, 1.0f);
                buffer.vertex(matrix, minimapPixelX + renderPixelSize, minimapPixelY + renderPixelSize, 0).color(r, g, b, 1.0f);
                buffer.vertex(matrix, minimapPixelX, minimapPixelY + renderPixelSize, 0).color(r, g, b, 1.0f);
            }
        }

        BufferRenderer.drawWithGlobalProgram(buffer.end());
    }

    private static int getMapStyleColor(World world, BlockPos pos) {
        // Create a cache key that only uses X and Z (not Y)
        BlockPos cacheKey = new BlockPos(pos.getX(), 0, pos.getZ());

        // Check cache first
        if (MAP_COLOR_CACHE.containsKey(cacheKey)) {
            return MAP_COLOR_CACHE.get(cacheKey);
        }

        int color = calculateMapColor(world, pos);
        MAP_COLOR_CACHE.put(cacheKey, color);
        return color;
    }

    private static int calculateMapColor(World world, BlockPos basePos) {
        try {
            // Find the top solid block at this position
            BlockPos.Mutable mutablePos = new BlockPos.Mutable(basePos.getX(), world.getTopY(), basePos.getZ());
            BlockState topBlockState = null;
            int topY = world.getBottomY();

            // Get the chunk first to ensure it's loaded
            WorldChunk chunk = world.getChunk(basePos.getX() >> 4, basePos.getZ() >> 4);
            if (chunk == null) {
                return 0x404040; // Dark gray for unloaded chunks
            }

            // Search down to find the first non-air block
            for (int y = Math.min(world.getTopY(), 320); y >= Math.max(world.getBottomY(), -64); y--) {
                mutablePos.setY(y);
                BlockState state = world.getBlockState(mutablePos);

                if (!state.isAir() && !state.isOf(Blocks.CAVE_AIR) && !state.isOf(Blocks.VOID_AIR)) {
                    topBlockState = state;
                    topY = y;
                    break;
                }
            }

            if (topBlockState == null || topBlockState.isAir()) {
                // If we can't find a solid block, check for water
                for (int y = Math.min(world.getTopY(), 320); y >= Math.max(world.getBottomY(), -64); y--) {
                    mutablePos.setY(y);
                    BlockState state = world.getBlockState(mutablePos);

                    if (state.isOf(Blocks.WATER)) {
                        return 0x4A90E2; // Water blue
                    }
                }
                return 0x404040; // Dark gray for void areas
            }

            // Get base color from block
            int baseColor = getBlockMapColor(topBlockState.getBlock());

            // Apply height-based shading similar to Minecraft maps
            int seaLevel = world.getSeaLevel();
            int heightDiff = topY - seaLevel;
            float shadeFactor = 1.0f + (heightDiff * 0.008f); // Subtle height shading
            shadeFactor = MathHelper.clamp(shadeFactor, 0.4f, 1.6f);

            // Apply biome tinting for grass and leaves
            if (shouldApplyBiomeTint(topBlockState.getBlock())) {
                baseColor = applyBiomeTint(baseColor, world, mutablePos.setY(topY));
            }

            // Apply shading
            int r = (int) (((baseColor >> 16) & 0xFF) * shadeFactor);
            int g = (int) (((baseColor >> 8) & 0xFF) * shadeFactor);
            int b = (int) ((baseColor & 0xFF) * shadeFactor);

            r = MathHelper.clamp(r, 0, 255);
            g = MathHelper.clamp(g, 0, 255);
            b = MathHelper.clamp(b, 0, 255);

            return (r << 16) | (g << 8) | b;

        } catch (Exception e) {
            // If anything goes wrong, return a default color
            return 0x808080; // Gray
        }
    }

    private static boolean shouldApplyBiomeTint(Block block) {
        return block == Blocks.GRASS_BLOCK ||
                block == Blocks.OAK_LEAVES ||
                block == Blocks.BIRCH_LEAVES ||
                block == Blocks.SPRUCE_LEAVES ||
                block == Blocks.JUNGLE_LEAVES ||
                block == Blocks.ACACIA_LEAVES ||
                block == Blocks.DARK_OAK_LEAVES ||
                block == Blocks.MANGROVE_LEAVES ||
                (block.toString().contains("leaves")); // Catch modded leaves
    }

    private static int getBlockMapColor(Block block) {
        // Water and liquids
        if (block == Blocks.WATER) return 0x4A90E2;
        if (block == Blocks.LAVA) return 0xFF6B1A;
        if (block == Blocks.ICE || block == Blocks.PACKED_ICE || block == Blocks.BLUE_ICE) return 0xA0C4E4;

        // Grass and vegetation
        if (block == Blocks.GRASS_BLOCK) return 0x7CB342;
        if (block == Blocks.DIRT || block == Blocks.COARSE_DIRT) return 0x976F3A;
        if (block == Blocks.PODZOL) return 0x594A2E;
        if (block == Blocks.MYCELIUM) return 0x705D75;
        if (block == Blocks.FARMLAND) return 0x976F3A;
        if (block == Blocks.DIRT_PATH) return 0xA8834A;

        // Sand and desert
        if (block == Blocks.SAND) return 0xF7E9A3;
        if (block == Blocks.RED_SAND) return 0xD68C59;
        if (block == Blocks.SANDSTONE) return 0xF7E9A3;
        if (block == Blocks.RED_SANDSTONE) return 0xD68C59;
        if (block == Blocks.TERRACOTTA) return 0xC47A5C;

        // Stone and ores
        if (block == Blocks.STONE || block == Blocks.COBBLESTONE) return 0x999999;
        if (block == Blocks.DEEPSLATE || block == Blocks.COBBLED_DEEPSLATE) return 0x646464;
        if (block == Blocks.GRANITE) return 0x9F6A42;
        if (block == Blocks.DIORITE) return 0xC4C4C4;
        if (block == Blocks.ANDESITE) return 0x8A8A8A;
        if (block == Blocks.BEDROCK) return 0x565656;
        if (block == Blocks.GRAVEL) return 0x8A8A8A;

        // Snow and ice
        if (block == Blocks.SNOW || block == Blocks.SNOW_BLOCK || block == Blocks.POWDER_SNOW) return 0xFFFEFE;

        // Wood logs
        if (block == Blocks.OAK_LOG || block == Blocks.OAK_WOOD || block == Blocks.STRIPPED_OAK_LOG) return 0x976F3A;
        if (block == Blocks.BIRCH_LOG || block == Blocks.BIRCH_WOOD || block == Blocks.STRIPPED_BIRCH_LOG) return 0xD7CA8B;
        if (block == Blocks.SPRUCE_LOG || block == Blocks.SPRUCE_WOOD || block == Blocks.STRIPPED_SPRUCE_LOG) return 0x6B4423;
        if (block == Blocks.JUNGLE_LOG || block == Blocks.JUNGLE_WOOD || block == Blocks.STRIPPED_JUNGLE_LOG) return 0x976F3A;
        if (block == Blocks.ACACIA_LOG || block == Blocks.ACACIA_WOOD || block == Blocks.STRIPPED_ACACIA_LOG) return 0xBA7E53;
        if (block == Blocks.DARK_OAK_LOG || block == Blocks.DARK_OAK_WOOD || block == Blocks.STRIPPED_DARK_OAK_LOG) return 0x4A2F17;

        // Try to handle newer wood types with fallback
        String blockName = block.toString();
        if (blockName.contains("cherry") && (blockName.contains("log") || blockName.contains("wood"))) return 0xE8B4CB;
        if (blockName.contains("mangrove") && (blockName.contains("log") || blockName.contains("wood"))) return 0x7A5543;

        // Leaves - base colors before biome tinting
        if (block == Blocks.OAK_LEAVES) return 0x59AE30;
        if (block == Blocks.BIRCH_LEAVES) return 0x8DB360;
        if (block == Blocks.SPRUCE_LEAVES) return 0x619A3C;
        if (block == Blocks.JUNGLE_LEAVES) return 0x30B95A;
        if (block == Blocks.ACACIA_LEAVES) return 0x9CAB3C;
        if (block == Blocks.DARK_OAK_LEAVES) return 0x2D5016;

        // Handle newer leaves with fallback
        if (blockName.contains("cherry") && blockName.contains("leaves")) return 0xF2B2D6;
        if (blockName.contains("mangrove") && blockName.contains("leaves")) return 0x59AE30;

        // Nether blocks
        if (block == Blocks.NETHERRACK) return 0x7A342A;
        if (block == Blocks.NETHER_BRICKS) return 0x2C1414;
        if (block == Blocks.SOUL_SAND || block == Blocks.SOUL_SOIL) return 0x4C3426;
        if (blockName.contains("crimson") && blockName.contains("nylium")) return 0x943F61;
        if (blockName.contains("warped") && blockName.contains("nylium")) return 0x167E86;

        // End blocks
        if (block == Blocks.END_STONE) return 0xE0D99A;
        if (block == Blocks.PURPUR_BLOCK) return 0xAB8AAB;

        // Ores (more vibrant colors for visibility)
        if (blockName.contains("coal_ore")) return 0x343434;
        if (blockName.contains("iron_ore")) return 0xD8AF93;
        if (blockName.contains("gold_ore")) return 0xFCEE4B;
        if (blockName.contains("diamond_ore")) return 0x5CDBD5;
        if (blockName.contains("emerald_ore")) return 0x00D93A;
        if (blockName.contains("redstone_ore")) return 0xD93A00;
        if (blockName.contains("lapis_ore")) return 0x4A4AFF;
        if (blockName.contains("copper_ore")) return 0xFF6A00;

        // Clay and concrete
        if (block == Blocks.CLAY) return 0xA3A3A3;

        // Common concrete colors
        if (blockName.contains("white") && blockName.contains("concrete")) return 0xD5D5D5;
        if (blockName.contains("black") && blockName.contains("concrete")) return 0x1D1D1D;
        if (blockName.contains("red") && blockName.contains("concrete")) return 0xB02E26;
        if (blockName.contains("green") && blockName.contains("concrete")) return 0x5E7C16;
        if (blockName.contains("blue") && blockName.contains("concrete")) return 0x3C44AA;
        if (blockName.contains("yellow") && blockName.contains("concrete")) return 0xF9D71C;

        // Default color for unknown blocks
        return 0x808080;
    }

    private static int applyBiomeTint(int baseColor, World world, BlockPos pos) {
        try {
            // Get biome at position
            var biomeRegistry = world.getBiome(pos);
            if (biomeRegistry == null) return baseColor;

            Biome biome = biomeRegistry.value();
            if (biome == null) return baseColor;

            // Get biome temperature
            float temperature = biome.getTemperature();

            // Simple humidity estimation based on temperature and biome characteristics
            float humidity = estimateBiomeHumidity(temperature);

            // Adjust color based on temperature and humidity
            int r = (baseColor >> 16) & 0xFF;
            int g = (baseColor >> 8) & 0xFF;
            int b = baseColor & 0xFF;

            // Cool biomes (temperature < 0.3) - more blue-green
            if (temperature < 0.3f) {
                g = (int) (g * 0.9f);
                b = (int) (b * 1.1f);
            }
            // Hot biomes (temperature > 0.9) - more yellow-brown
            else if (temperature > 0.9f) {
                r = (int) (r * 1.1f);
                g = (int) (g * 0.95f);
                b = (int) (b * 0.8f);
            }

            // Dry biomes (low humidity) - less vibrant
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

    private static float estimateBiomeHumidity(float temperature) {
        // Simplified humidity estimation based on temperature
        if (temperature < 0.2f) {
            return 0.2f; // Cold biomes tend to be dry
        } else if (temperature > 1.0f) {
            return 0.1f; // Very hot biomes tend to be dry (deserts)
        } else if (temperature > 0.5f && temperature < 0.8f) {
            return 0.6f; // Temperate biomes have moderate humidity
        } else {
            return 0.4f; // Default moderate humidity
        }
    }

    private static void drawMinimapBorder(DrawContext context, int x, int y) {
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

        BufferRenderer.drawWithGlobalProgram(buffer.end());

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

        BufferRenderer.drawWithGlobalProgram(buffer.end());
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