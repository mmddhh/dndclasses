package mdh.dndclasses.worldgen.terrain;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Coarse terrain grid shared by the climate/civilization prototype and diagnostics. */
public record TerrainGrid(WorldTopology topology, long seed, short[] height, byte[] biome,
                          float[] temperature, float[] humidity, boolean[] water,
                          float[] habitability) {

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
        int scale = 4;
        BufferedImage terrain = new BufferedImage(size * scale, size * scale,
                BufferedImage.TYPE_INT_RGB);
        BufferedImage habitabilityImage = new BufferedImage(size * scale, size * scale,
                BufferedImage.TYPE_INT_RGB);

        for (int z = 0; z < size; z++) {
            for (int x = 0; x < size; x++) {
                int index = index(x, z);
                Color color = terrainColor(index);
                Color habitColor = habitabilityColor(index);
                for (int dx = 0; dx < scale; dx++) {
                    for (int dz = 0; dz < scale; dz++) {
                        terrain.setRGB(x * scale + dx, z * scale + dz, color.getRGB());
                        habitabilityImage.setRGB(x * scale + dx, z * scale + dz,
                                habitColor.getRGB());
                    }
                }
            }
        }
        ImageIO.write(terrain, "png", directory.resolve("terrain-" + seed + ".png").toFile());
        ImageIO.write(habitabilityImage, "png",
                directory.resolve("habitability-" + seed + ".png").toFile());
    }

    private Color terrainColor(int index) {
        if (water[index]) {
            return new Color(38, 92, 150);
        }
        return switch (TerrainModel.Biome.values()[biome[index] & 0xff]) {
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
        float value = Math.max(0.0f, Math.min(1.0f, habitability[index]));
        int red = (int) (30 + value * 225);
        int green = (int) (30 + value * 180);
        int blue = (int) (30 + value * 40);
        return new Color(red, green, blue);
    }
}
