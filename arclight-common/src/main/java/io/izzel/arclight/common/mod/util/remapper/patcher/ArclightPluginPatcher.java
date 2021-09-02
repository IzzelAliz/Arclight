package io.izzel.arclight.common.mod.util.remapper.patcher;

import io.izzel.arclight.api.PluginPatcher;
import io.izzel.arclight.common.mod.ArclightMod;
import io.izzel.arclight.common.mod.util.remapper.ClassLoaderRemapper;
import io.izzel.arclight.common.mod.util.remapper.GlobalClassRepo;
import io.izzel.arclight.common.mod.util.remapper.PluginTransformer;
import org.bukkit.configuration.file.YamlConfiguration;
import org.objectweb.asm.tree.ClassNode;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class ArclightPluginPatcher implements PluginTransformer {

    private final List<PluginPatcher> list;

    public ArclightPluginPatcher(List<PluginPatcher> list) {
        this.list = list;
    }

    @Override
    public void handleClass(ClassNode node, ClassLoaderRemapper remapper) {
        for (PluginPatcher patcher : list) {
            patcher.handleClass(node, GlobalClassRepo.INSTANCE);
        }
    }

    public static List<PluginPatcher> load(List<PluginTransformer> transformerList) {
        File pluginFolder = new File("plugins");
        if (pluginFolder.exists()) {
            ArclightMod.LOGGER.info("patcher.loading");
            ArrayList<PluginPatcher> list = new ArrayList<>();
            File[] files = pluginFolder.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile() && file.getName().endsWith(".jar")) {
                        loadFromJar(file).ifPresent(list::add);
                    }
                }
                if (!list.isEmpty()) {
                    list.sort(Comparator.comparing(PluginPatcher::priority));
                    ArclightMod.LOGGER.info("patcher.loaded", list.size());
                    transformerList.add(new ArclightPluginPatcher(list));
                    return list;
                }
            }
        }
        return Collections.emptyList();
    }

    private static Optional<PluginPatcher> loadFromJar(File file) {
        try (JarFile jarFile = new JarFile(file)) {
            JarEntry jarEntry = jarFile.getJarEntry("plugin.yml");
            if (jarEntry != null) {
                try (InputStream stream = jarFile.getInputStream(jarEntry)) {
                    YamlConfiguration configuration = YamlConfiguration.loadConfiguration(new InputStreamReader(stream));
                    String name = configuration.getString("arclight.patcher");
                    if (name != null) {
                        URLClassLoader loader = new URLClassLoader(new URL[]{file.toURI().toURL()}, ArclightPluginPatcher.class.getClassLoader());
                        Class<?> clazz = Class.forName(name, false, loader);
                        PluginPatcher patcher = clazz.asSubclass(PluginPatcher.class).getConstructor().newInstance();
                        return Optional.of(patcher);
                    }
                }
            }
        } catch (Throwable e) {
            ArclightMod.LOGGER.debug("patcher.load-error", e);
        }
        return Optional.empty();
    }
}
