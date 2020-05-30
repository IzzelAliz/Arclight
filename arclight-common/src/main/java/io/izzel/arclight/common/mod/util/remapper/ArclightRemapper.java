package io.izzel.arclight.common.mod.util.remapper;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import io.izzel.arclight.api.Unsafe;
import net.md_5.specialsource.InheritanceMap;
import net.md_5.specialsource.JarMapping;
import net.md_5.specialsource.provider.ClassLoaderProvider;
import net.md_5.specialsource.provider.JointProvider;
import org.bukkit.plugin.java.JavaPluginLoader;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URLClassLoader;

public class ArclightRemapper {

    public static final ArclightRemapper INSTANCE;

    static {
        try {
            INSTANCE = new ArclightRemapper();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private final JarMapping toNmsMapping;
    private final JarMapping toBukkitMapping;
    public final InheritanceMap inheritanceMap;

    public ArclightRemapper() throws Exception {
        this.toNmsMapping = new JarMapping();
        this.toBukkitMapping = new JarMapping();
        this.inheritanceMap = new InheritanceMap();
        this.toNmsMapping.loadMappings(
            new BufferedReader(new InputStreamReader(ArclightRemapper.class.getResourceAsStream("/bukkit_srg.srg"))),
            null, null, false
        );
        this.toBukkitMapping.loadMappings(
            new BufferedReader(new InputStreamReader(ArclightRemapper.class.getResourceAsStream("/bukkit_srg.srg"))),
            null, null, true
        );
        BiMap<String, String> inverseClassMap = HashBiMap.create(toNmsMapping.classes).inverse();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(ArclightRemapper.class.getResourceAsStream("/inheritanceMap.txt")))) {
            inheritanceMap.load(reader, inverseClassMap);
        }
        JointProvider inheritanceProvider = new JointProvider();
        inheritanceProvider.add(inheritanceMap);
        inheritanceProvider.add(new ClassLoaderProvider(ClassLoader.getSystemClassLoader()));
        this.toNmsMapping.setFallbackInheritanceProvider(inheritanceProvider);
        this.toBukkitMapping.setFallbackInheritanceProvider(inheritanceProvider);
    }

    public PluginRemapper createPluginRemapper(JavaPluginLoader loader, URLClassLoader plugin) {
        return new PluginRemapper(copyOf(toNmsMapping), copyOf(toBukkitMapping), loader, plugin);
    }

    private static long pkgOffset, clOffset, mdOffset, fdOffset, mapOffset;

    static {
        try {
            pkgOffset = Unsafe.objectFieldOffset(JarMapping.class.getField("packages"));
            clOffset = Unsafe.objectFieldOffset(JarMapping.class.getField("classes"));
            mdOffset = Unsafe.objectFieldOffset(JarMapping.class.getField("methods"));
            fdOffset = Unsafe.objectFieldOffset(JarMapping.class.getField("fields"));
            mapOffset = Unsafe.objectFieldOffset(JarMapping.class.getDeclaredField("inheritanceMap"));
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
    }

    private JarMapping copyOf(JarMapping mapping) {
        JarMapping jarMapping = new JarMapping();
        Unsafe.putObject(jarMapping, pkgOffset, Unsafe.getObject(mapping, pkgOffset));
        Unsafe.putObject(jarMapping, clOffset, Unsafe.getObject(mapping, clOffset));
        Unsafe.putObject(jarMapping, mdOffset, Unsafe.getObject(mapping, mdOffset));
        Unsafe.putObject(jarMapping, fdOffset, Unsafe.getObject(mapping, fdOffset));
        Unsafe.putObject(jarMapping, mapOffset, Unsafe.getObject(mapping, mapOffset));
        return jarMapping;
    }
}
