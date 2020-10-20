package io.izzel.arclight.common.mod.util.remapper;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import io.izzel.arclight.api.Unsafe;
import io.izzel.arclight.common.mod.util.log.ArclightI18nLogger;
import io.izzel.arclight.common.mod.util.remapper.patcher.ArclightPluginPatcher;
import net.md_5.specialsource.InheritanceMap;
import net.md_5.specialsource.JarMapping;
import net.md_5.specialsource.provider.ClassLoaderProvider;
import net.md_5.specialsource.provider.JointProvider;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class ArclightRemapper {

    public static final ArclightRemapper INSTANCE;

    static {
        ArclightI18nLogger.getLogger("Arclight").info("loading-mapping");
        try {
            INSTANCE = new ArclightRemapper();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private final JarMapping toNmsMapping;
    private final JarMapping toBukkitMapping;
    public final InheritanceMap inheritanceMap;
    private final List<PluginTransformer> transformerList = new ArrayList<>();

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
        this.transformerList.add(ArclightInterfaceInvokerGen.INSTANCE);
        this.transformerList.add(ArclightRedirectAdapter.INSTANCE);
        this.transformerList.add(ClassLoaderAdapter.INSTANCE);
        ArclightPluginPatcher.load(this.transformerList);
    }

    public static ClassLoaderRemapper createClassLoaderRemapper(ClassLoader classLoader) {
        return new ClassLoaderRemapper(INSTANCE.copyOf(INSTANCE.toNmsMapping), INSTANCE.copyOf(INSTANCE.toBukkitMapping), classLoader);
    }

    public List<PluginTransformer> getTransformerList() {
        return transformerList;
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
