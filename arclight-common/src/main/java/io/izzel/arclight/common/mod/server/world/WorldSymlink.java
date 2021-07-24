package io.izzel.arclight.common.mod.server.world;

import io.izzel.arclight.common.mod.ArclightMod;
import org.bukkit.Bukkit;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.world.level.storage.DerivedLevelData;

public class WorldSymlink {

    public static void create(DerivedLevelData worldInfo, File dimensionFolder) {
        String name = worldInfo.getLevelName();
        Path source = new File(Bukkit.getWorldContainer(), name).toPath();
        Path dest = dimensionFolder.toPath();
        try {
            if (!Files.isSymbolicLink(source)) {
                if (Files.exists(source)) {
                    ArclightMod.LOGGER.warn("symlink-file-exist", source);
                    return;
                }
                Files.createSymbolicLink(source, dest);
            }
        } catch (UnsupportedOperationException e) {
            ArclightMod.LOGGER.warn("error-symlink", e);
        } catch (IOException e) {
            ArclightMod.LOGGER.error("Error creating symlink", e);
        }
    }
}
