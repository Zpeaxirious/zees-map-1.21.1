package net.yumeverse.zeesmap.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.*;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.yumeverse.zeesmap.storage.WaypointStorage;
import org.joml.Matrix4f;

import java.util.HashMap;
import java.util.Map;

public class MinimapRenderer {
    private static final int MINIMAP_SIZE = 128;
    private static final int MINIMAP_RADIUS = MINIMAP_SIZE / 2;
    private static final int RENDER_DISTANCE = 64; // blocks to render in each direction

    // Cache for block colors to avoid recalculating
    private static final Map<Block, Integer> BLOCK_COLOR_CACHE = new HashMap<>();

    // Player arrow texture (we'll draw this manually)
    private static final Identifier WAYPOINT_ICON = Identifier.of("zees-map", "textures/gui/waypoint_icon.png");

    static {
        initializeBlockColors();
    }

    private static void initializeBlockColors() {
        // Basic block colors - you can expand this
        BLOCK_COLOR_CACHE.put(Blocks.GRASS_BLOCK, 0x7CB342);
        BLOCK_COLOR_CACHE.put(Blocks.DIRT, 0x8D6E63);
        BLOCK_COLOR_CACHE.put(Blocks.STONE, 0x9E9E9E);
        BLOCK_COLOR_CACHE.put(Blocks.WATER, 0x2196F3);
        BLOCK_COLOR_CACHE.put(Blocks.SAND, 0xF5DEB3);
        BLOCK_COLOR_CACHE.put(Blocks.SNOW, 0xFFFFFF);
        BLOCK_COLOR_CACHE.put(Blocks.ICE, 0xB3E5FC);
        BLOCK_COLOR_CACHE.put(Blocks.LAVA, 0xFF5722);
        BLOCK_COLOR_CACHE.put(Blocks.COBBLESTONE, 0x757575);
        BLOCK_COLOR_CACHE.put(Blocks.BEDROCK, 0x424242);
        BLOCK_COLOR_CACHE.put(Blocks.COAL_ORE, 0x37474F);
        BLOCK_COLOR_CACHE.put(Blocks.IRON_ORE, 0xBCAAA4);
        BLOCK_COLOR_CACHE.put(Blocks.GOLD_ORE, 0xFFD600);
        BLOCK_COLOR_CACHE.put(Blocks.DIAMOND_ORE, 0x4FC3F7);
        BLOCK_COLOR_CACHE.put(Blocks.EMERALD_ORE, 0x4CAF50);
        BLOCK_COLOR_CACHE.put(Blocks.REDSTONE_ORE, 0xF44336);
        BLOCK_COLOR_CACHE.put(Blocks.DEEPSLATE, 0x4A4A4A);
        BLOCK_COLOR_CACHE.put(Blocks.NETHERRACK, 0x8D4A4A);
        BLOCK_COLOR_CACHE.put(Blocks.END_STONE, 0xFFF8E1);
        BLOCK_COLOR_CACHE.put(Blocks.OAK_LOG, 0x8D6E63);
        BLOCK_COLOR_CACHE.put(Blocks.OAK_LEAVES, 0x66BB6A);
        BLOCK_COLOR_CACHE.put(Blocks.BIRCH_LOG, 0xF5F5DC);
        BLOCK_COLOR_CACHE.put(Blocks.BIRCH_LEAVES, 0x8BC34A);
        BLOCK_COLOR_CACHE.put(Blocks.SPRUCE_LOG, 0x5D4037);
        BLOCK_COLOR_CACHE.put(Blocks.SPRUCE_LEAVES, 0x2E7D32);
        BLOCK_COLOR_CACHE.put(Blocks.JUNGLE_LOG, 0x8D6E63);
        BLOCK_COLOR_CACHE.put(Blocks.JUNGLE_LEAVES, 0x43A047);
        BLOCK_COLOR_CACHE.put(Blocks.ACACIA_LOG, 0xD84315);
        BLOCK_COLOR_CACHE.put(Blocks.ACACIA_LEAVES, 0x689F38);
        BLOCK_COLOR_CACHE.put(Blocks.DARK_OAK_LOG, 0x3E2723);
        BLOCK_COLOR_CACHE.put(Blocks.DARK_OAK_LEAVES, 0x1B5E20);
        BLOCK_COLOR_CACHE.put(Blocks.CHERRY_LOG, 0xF8BBD9);
        BLOCK_COLOR_CACHE.put(Blocks.CHERRY_LEAVES, 0xF48FB1);
        BLOCK_COLOR_CACHE.put(Blocks.MANGROVE_LOG, 0x8D6E63);
        BLOCK_COLOR_CACHE.put(Blocks.MANGROVE_LEAVES, 0x4CAF50);
        BLOCK_COLOR_CACHE.put(Blocks.BAMBOO, 0x8BC34A);
    }

    public static void render(DrawContext context, float tickDelta) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) return;

        int screenWidth = client.getWindow().getScaledWidth();
        int screenHeight = client.getWindow().getScaledHeight();

        // Position minimap in top-right corner
        int minimapX = screenWidth - MINIMAP_SIZE - 10;
        int minimapY = 10;

        // Draw minimap background (black circle)
        drawCircleBackground(context, minimapX, minimapY);

        // Render the world from above
        renderWorldFromAbove(context, client, minimapX, minimapY, tickDelta);

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
        // Draw black circle background
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

    private static void renderWorldFromAbove(DrawContext context, MinecraftClient client, int minimapX, int minimapY, float tickDelta) {
        World world = client.world;
        if (world == null) return;

        double playerX = client.player.getX();
        double playerZ = client.player.getZ();
        int playerY = (int) client.player.getY();

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

                // Get the top block at this position
                BlockPos pos = new BlockPos(worldX, playerY, worldZ);
                Block topBlock = getTopBlock(world, pos);

                int color = getBlockColor(topBlock, world, pos);
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

    private static Block getTopBlock(World world, BlockPos startPos) {
        // Search down from a reasonable height to find the top solid block
        for (int y = Math.min(startPos.getY() + 10, world.getTopY()); y >= world.getBottomY(); y--) {
            BlockPos pos = new BlockPos(startPos.getX(), y, startPos.getZ());
            Block block = world.getBlockState(pos).getBlock();

            if (!block.equals(Blocks.AIR) && !block.equals(Blocks.CAVE_AIR)) {
                return block;
            }
        }
        return Blocks.STONE; // Default fallback
    }

    private static int getBlockColor(Block block, World world, BlockPos pos) {
        // Check cache first
        if (BLOCK_COLOR_CACHE.containsKey(block)) {
            return BLOCK_COLOR_CACHE.get(block);
        }

        // For grass blocks, try to get biome color
        if (block.equals(Blocks.GRASS_BLOCK)) {
            try {
                Biome biome = world.getBiome(pos).value();
                // This is a simplified approach - you could implement proper biome coloring
                return 0x7CB342; // Default grass color
            } catch (Exception e) {
                return 0x7CB342;
            }
        }

        // Default color for unknown blocks
        return 0x8E8E8E;
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