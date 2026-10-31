package mdh.dndclasses.client;

import net.minecraftforge.fml.loading.FMLPaths;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.stream.Stream;

/**
 * Dev-only helper: copies the master AUI pages ({@code <project>/apricity}) into the game directory
 * ({@code run/apricity}) before any screen loads.
 * <p>
 * The Gradle {@code runClient} task already has a {@code syncApricity} copy step, but a plain
 * IDEA Application run configuration bypasses Gradle, leaving the runtime on stale pages. This runs
 * for every launch and is a no-op in production (the source directory does not exist there).
 */
public final class DevAssets {

    private DevAssets() {
    }

    public static void syncApricity() {
        Path gameDir = FMLPaths.GAMEDIR.get();
        Path source = gameDir.resolve("..").resolve("apricity").normalize();
        Path target = gameDir.resolve("apricity").normalize();
        if (!Files.isDirectory(source) || !Files.isDirectory(target)) {
            return;
        }
        try (Stream<Path> stream = Files.walk(source)) {
            stream.filter(Files::isRegularFile).forEach(path -> {
                Path destination = target.resolve(source.relativize(path).toString());
                try {
                    Files.createDirectories(destination.getParent());
                    Files.copy(path, destination, StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException ignored) {
                    // A page may be locked by the running client; skip it, it will sync next launch.
                }
            });
        } catch (IOException ignored) {
        }
    }
}
