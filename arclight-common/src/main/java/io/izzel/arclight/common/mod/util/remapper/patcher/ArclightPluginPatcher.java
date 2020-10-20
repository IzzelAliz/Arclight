package io.izzel.arclight.common.mod.util.remapper.patcher;

import io.izzel.arclight.common.mod.ArclightMod;
import io.izzel.arclight.common.mod.util.remapper.ClassLoaderRemapper;
import io.izzel.arclight.common.mod.util.remapper.PluginTransformer;
import org.bukkit.configuration.file.YamlConfiguration;
import org.objectweb.asm.tree.ClassNode;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class ArclightPluginPatcher implements PluginTransformer {

    private final List<PluginTransformer> list;

    public ArclightPluginPatcher(List<PluginTransformer> list) {
        this.list = list;
    }

    @Override
    public void handleClass(ClassNode node, ClassLoaderRemapper remapper) {
        for (PluginTransformer transformer : list) {
            transformer.handleClass(node, remapper);
        }
    }

    public static void load(List<PluginTransformer> transformerList) {
        File pluginFolder = new File("plugins");
        if (pluginFolder.exists()) {
            ArclightMod.LOGGER.info("patcher.loading");
            ArrayList<PluginTransformer> list = new ArrayList<>();
            for (File file : pluginFolder.listFiles()) {
                if (file.isFile() && file.getName().endsWith(".jar")) {
                    loadFromJar(file.toPath()).ifPresent(list::add);
                }
            }
            if (!list.isEmpty()) {
                list.sort(Comparator.comparing(PluginTransformer::priority));
                ArclightMod.LOGGER.info("patcher.loaded", list.size());
                transformerList.add(new ArclightPluginPatcher(list));
            }
        }
    }

    private static Optional<PluginTransformer> loadFromJar(Path path) {
        try {
            FileSystem fileSystem = FileSystems.newFileSystem(path, ArclightPluginPatcher.class.getClassLoader());
            Path pluginYml = fileSystem.getPath("plugin.yml");
            if (Files.exists(pluginYml)) {
                YamlConfiguration configuration = YamlConfiguration.loadConfiguration(Files.newBufferedReader(pluginYml));
                String name = configuration.getString("arclight.patcher");
                if (name != null) {
                    URLClassLoader loader = new URLClassLoader(new URL[]{path.toUri().toURL()}, ArclightPluginPatcher.class.getClassLoader());
                    Class<?> clazz = Class.forName(name, false, loader);
                    PluginTransformer transformer = clazz.asSubclass(PluginTransformer.class).newInstance();
                    return Optional.of(transformer);
                }
            }
        } catch (Exception e) {
            ArclightMod.LOGGER.error("patcher.load-error", e);
        }
        return Optional.empty();
    }
}
