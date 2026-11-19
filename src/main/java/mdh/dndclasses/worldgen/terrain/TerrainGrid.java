package mdh.dndclasses.worldgen.terrain;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Coarse terrain grid shared by the climate/civilization prototype and diagnostics. */
public record TerrainGrid(WorldTopology topology, long seed, short[] height, byte[] biome,
                          float[] temperature, float[] humidity, boolean[] water,
                          boolean[] lake, boolean[] river, int[] riverFlow,
                          java.util.List<RiverPath> riverPaths, float[] habitability) {

    private static final int SCALE = 4;
    private static final int LEGEND_WIDTH = 340;

    public int size() {
        return topology.gridSize();
    }

    public int index(int x, int z) {
        return z * size() + x;
    }

    public TerrainModel.Biome biomeAt(int x, int z) {
        return TerrainModel.Biome.values()[biome[index(x, z)] & 0xff];
    }

    public void writeImages(Path directory) throws IOException {
        Files.createDirectories(directory);
        int size = size();
        int mapSize = size * SCALE;
        BufferedImage terrain = new BufferedImage(mapSize + LEGEND_WIDTH, mapSize,
                BufferedImage.TYPE_INT_RGB);
        BufferedImage habitabilityImage = new BufferedImage(mapSize + LEGEND_WIDTH, mapSize,
                BufferedImage.TYPE_INT_RGB);
        BufferedImage heightImage = new BufferedImage(mapSize + LEGEND_WIDTH, mapSize,
                BufferedImage.TYPE_INT_RGB);

        Graphics2D terrainGraphics = terrain.createGraphics();
        Graphics2D habitabilityGraphics = habitabilityImage.createGraphics();
        Graphics2D heightGraphics = heightImage.createGraphics();
        configure(terrainGraphics);
        configure(habitabilityGraphics);
        configure(heightGraphics);

        terrainGraphics.setColor(Color.BLACK);
        terrainGraphics.fillRect(0, 0, terrain.getWidth(), terrain.getHeight());
        habitabilityGraphics.setColor(Color.BLACK);
        habitabilityGraphics.fillRect(0, 0, habitabilityImage.getWidth(),
                habitabilityImage.getHeight());
        heightGraphics.setColor(Color.BLACK);
        heightGraphics.fillRect(0, 0, heightImage.getWidth(), heightImage.getHeight());

        for (int z = 0; z < size; z++) {
            for (int x = 0; x < size; x++) {
                int index = index(x, z);
                terrainGraphics.setColor(terrainColor(index));
                terrainGraphics.fillRect(x * SCALE, z * SCALE, SCALE, SCALE);
                habitabilityGraphics.setColor(habitabilityColor(index));
                habitabilityGraphics.fillRect(x * SCALE, z * SCALE, SCALE, SCALE);
                heightGraphics.setColor(heightColor(index));
                heightGraphics.fillRect(x * SCALE, z * SCALE, SCALE, SCALE);
            }
        }

        drawMapFrame(terrainGraphics, mapSize, size);
        drawMapFrame(habitabilityGraphics, mapSize, size);
        drawMapFrame(heightGraphics, mapSize, size);
        drawRiverCurves(terrainGraphics);
        drawRiverCurves(heightGraphics);
        drawTerrainLegend(terrainGraphics, mapSize);
        drawHabitabilityLegend(habitabilityGraphics, mapSize);
        drawHeightLegend(heightGraphics, mapSize);

        ImageIO.write(terrain, "png", directory.resolve("terrain-" + seed + ".png").toFile());
        ImageIO.write(habitabilityImage, "png",
                directory.resolve("habitability-" + seed + ".png").toFile());
        ImageIO.write(heightImage, "png",
                directory.resolve("heightmap-" + seed + ".png").toFile());
    }

    private void drawRiverCurves(Graphics2D graphics) {
        graphics.setColor(new Color(45, 120, 190, 230));
        graphics.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND,
                BasicStroke.JOIN_ROUND));
        int size = size();
        for (RiverPath path : riverPaths) {
            if (path.points().size() < 2) {
                continue;
            }
            java.awt.geom.Path2D.Double line = null;
            RiverPath.Node previous = null;
            for (RiverPath.Node node : path.points()) {
                boolean seamJump = previous != null
                        && (Math.abs(node.x() - previous.x()) > size / 2
                        || Math.abs(node.z() - previous.z()) > size / 2);
                if (line == null || seamJump) {
                    if (line != null) {
                        graphics.draw(line);
                    }
                    line = new java.awt.geom.Path2D.Double();
                    line.moveTo((node.x() + 0.5) * SCALE, (node.z() + 0.5) * SCALE);
                } else {
                    line.lineTo((node.x() + 0.5) * SCALE, (node.z() + 0.5) * SCALE);
                }
                previous = node;
            }
            if (line != null) {
                graphics.draw(line);
            }
        }
    }

    private void drawMapFrame(Graphics2D graphics, int mapSize, int size) {
        int center = mapSize / 2;
        graphics.setColor(new Color(255, 255, 255, 180));
        graphics.setStroke(new BasicStroke(2f));
        graphics.drawLine(center, 0, center, mapSize);
        graphics.drawLine(0, center, mapSize, center);
        graphics.setColor(Color.WHITE);
        graphics.setFont(new Font(Font.MONOSPACED, Font.BOLD, 14));
        graphics.drawString("(0,0)", center + 8, center - 8);
        graphics.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        graphics.drawString("x -31,360", 8, mapSize - 8);
        graphics.drawString("x +31,360", mapSize - 110, mapSize - 8);
        graphics.drawString("z -31,360", 8, 18);
        graphics.drawString("z +31,360", 8, mapSize - 28);
        graphics.drawString("canonical 0,0 = top-left; center marked (0,0)", 8, mapSize - 44);
        graphics.drawString("center (0,0) = south pole; top/bottom seam = north pole",
                8, mapSize - 60);
        graphics.drawString("equator climate at z = -15,680 and z = +15,680",
                8, mapSize - 76);
    }

    private void drawTerrainLegend(Graphics2D graphics, int mapSize) {
        int left = mapSize + 16;
        int top = 24;
        graphics.setColor(Color.WHITE);
        graphics.setFont(new Font(Font.MONOSPACED, Font.BOLD, 16));
        graphics.drawString("Terrain / biome", left, top);
        graphics.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        TerrainModel.Biome[] biomes = TerrainModel.Biome.values();
        int y = top + 28;
        for (TerrainModel.Biome biome : biomes) {
            graphics.setColor(terrainColorForBiome(biome));
            graphics.fillRect(left, y - 12, 14, 14);
            graphics.setColor(Color.WHITE);
            graphics.drawString(biome.name().toLowerCase(), left + 22, y);
            y += 20;
        }
        graphics.setColor(new Color(45, 120, 190));
        graphics.fillRect(left, y - 12, 14, 14);
        graphics.setColor(Color.WHITE);
        graphics.drawString("river", left + 22, y);
        y += 20;
        graphics.setColor(new Color(55, 130, 175));
        graphics.fillRect(left, y - 12, 14, 14);
        graphics.setColor(Color.WHITE);
        graphics.drawString("lake", left + 22, y);
        y += 20;
        graphics.setColor(new Color(200, 200, 200));
        graphics.drawString("white cross = world center (0,0)", left, y + 18);
        graphics.drawString("image edges wrap into each other", left, y + 38);
        graphics.drawString("water: light shallow / dark deep", left, y + 58);
    }

    private void drawHeightLegend(Graphics2D graphics, int mapSize) {
        int left = mapSize + 16;
        int top = 24;
        graphics.setColor(Color.WHITE);
        graphics.setFont(new Font(Font.MONOSPACED, Font.BOLD, 16));
        graphics.drawString("Height / rivers", left, top);
        graphics.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        graphics.drawString("dark low -> light high", left, top + 24);

        int barX = left;
        int barY = top + 44;
        int barWidth = 34;
        int barHeight = 300;
        for (int i = 0; i < barHeight; i++) {
            float value = 1.0f - i / (float) (barHeight - 1);
            graphics.setColor(landHeightColor(value));
            graphics.fillRect(barX, barY + i, barWidth, 1);
        }
        graphics.setColor(Color.WHITE);
        graphics.drawString("320", barX + barWidth + 8, barY + 12);
        graphics.drawString("140", barX + barWidth + 8, barY + barHeight / 2);
        graphics.drawString("63 sea", barX + barWidth + 8, barY + barHeight);
        graphics.setColor(new Color(45, 120, 190));
        graphics.fillRect(barX, barY + barHeight + 22, 14, 14);
        graphics.setColor(Color.WHITE);
        graphics.drawString("river", barX + 22, barY + barHeight + 34);
        graphics.setColor(new Color(200, 200, 200));
        graphics.drawString("blue = water/river", left, barY + barHeight + 58);
        graphics.drawString("white cross = world center (0,0)", left, barY + barHeight + 78);
    }

    private Color heightColor(int index) {
        if (river[index]) {
            return new Color(45, 120, 190);
        }
        if (lake[index]) {
            return new Color(55, 130, 175);
        }
        if (water[index]) {
            int depth = Math.max(0, TerrainModel.SEA_LEVEL - height[index]);
            float t = Math.min(1.0f, depth / 110.0f);
            return new Color((int) (80 - 65 * t), (int) (155 - 110 * t),
                    (int) (210 - 130 * t));
        }
        float t = Math.max(0.0f, Math.min(1.0f,
                (height[index] - TerrainModel.SEA_LEVEL) / 160.0f));
        return landHeightColor(t);
    }

    private static Color landHeightColor(float raw) {
        float value = Math.max(0.0f, Math.min(1.0f, raw));
        int red = (int) (90 + 150 * value);
        int green = (int) (105 + 120 * value);
        int blue = (int) (65 + 110 * value);
        return new Color(red, green, blue);
    }

    private void drawHabitabilityLegend(Graphics2D graphics, int mapSize) {        int left = mapSize + 16;
        int top = 24;
        graphics.setColor(Color.WHITE);
        graphics.setFont(new Font(Font.MONOSPACED, Font.BOLD, 16));
        graphics.drawString("Habitability", left, top);
        graphics.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        graphics.drawString("dark = low, bright yellow = high", left, top + 24);

        int barX = left;
        int barY = top + 42;
        int barWidth = 34;
        int barHeight = 260;
        for (int i = 0; i < barHeight; i++) {
            float value = 1.0f - i / (float) (barHeight - 1);
            graphics.setColor(landHabitabilityColor(value));
            graphics.fillRect(barX, barY + i, barWidth, 1);
        }
        graphics.setColor(Color.WHITE);
        graphics.drawString("1.0", barX + barWidth + 8, barY + 12);
        graphics.drawString("0.5", barX + barWidth + 8, barY + barHeight / 2);
        graphics.drawString("0.0", barX + barWidth + 8, barY + barHeight);
        graphics.setColor(new Color(38, 92, 150));
        graphics.fillRect(barX, barY + barHeight + 22, 14, 14);
        graphics.setColor(Color.WHITE);
        graphics.drawString("water (habitability 0)", left + 22, barY + barHeight + 34);
        graphics.drawString("rivers also count as water", left, barY + barHeight + 52);
        graphics.drawString("white cross = world center (0,0)", left, barY + barHeight + 74);
    }

    private void configure(Graphics2D graphics) {
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    }

    private Color terrainColor(int index) {
        if (river[index]) {
            return new Color(45, 120, 190);
        }
        if (lake[index]) {
            return new Color(55, 130, 175);
        }
        if (water[index]) {
            int depth = Math.max(0, TerrainModel.SEA_LEVEL - height[index]);
            float t = Math.min(1.0f, depth / 110.0f);
            return new Color(
                    (int) (80 - 65 * t),
                    (int) (155 - 110 * t),
                    (int) (210 - 130 * t));
        }
        return terrainColorForBiome(TerrainModel.Biome.values()[biome[index] & 0xff]);
    }

    private static Color terrainColorForBiome(TerrainModel.Biome biome) {
        return switch (biome) {
            case OCEAN -> new Color(38, 92, 150);
            case BEACH -> new Color(210, 196, 140);
            case TUNDRA -> new Color(170, 185, 190);
            case TAIGA -> new Color(70, 105, 85);
            case FOREST -> new Color(55, 120, 70);
            case PLAINS -> new Color(125, 170, 90);
            case SAVANNA -> new Color(175, 165, 90);
            case JUNGLE -> new Color(35, 135, 75);
            case SWAMP -> new Color(75, 105, 70);
            case DESERT -> new Color(205, 175, 110);
            case HILLS -> new Color(115, 130, 85);
            case MOUNTAINS -> new Color(145, 145, 140);
        };
    }

    private Color habitabilityColor(int index) {
        if (water[index]) {
            return new Color(20, 45, 75);
        }
        return landHabitabilityColor(habitability[index]);
    }

    private static Color landHabitabilityColor(float raw) {
        float value = Math.max(0.0f, Math.min(1.0f, raw));
        int red = (int) (30 + value * 225);
        int green = (int) (30 + value * 180);
        int blue = (int) (30 + value * 40);
        return new Color(red, green, blue);
    }
}
