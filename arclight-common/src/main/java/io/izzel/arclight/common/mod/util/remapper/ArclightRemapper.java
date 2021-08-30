package io.izzel.arclight.common.mod.util.remapper;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import io.izzel.arclight.api.PluginPatcher;
import io.izzel.arclight.api.Unsafe;
import io.izzel.arclight.common.mod.util.log.ArclightI18nLogger;
import io.izzel.arclight.common.mod.util.remapper.patcher.ArclightPluginPatcher;
import io.izzel.arclight.common.mod.util.remapper.resource.RemapSourceHandler;
import net.md_5.specialsource.InheritanceMap;
import net.md_5.specialsource.JarMapping;
import net.md_5.specialsource.JarRemapper;
import net.md_5.specialsource.provider.ClassLoaderProvider;
import net.md_5.specialsource.provider.JointProvider;
import org.apache.commons.io.FileUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

@SuppressWarnings("unchecked")
public class ArclightRemapper {

    public static final ArclightRemapper INSTANCE;
    public static final File DUMP;
    public static final Function<byte[], byte[]> SWITCH_TABLE_FIXER;

    static {
        ArclightI18nLogger.getLogger("Arclight").info("loading-mapping");
        try {
            INSTANCE = new ArclightRemapper();
            String property = System.getProperty("arclight.remapper.dump");
            if (property != null) {
                DUMP = new File(property);
                if (!DUMP.exists()) {
                    DUMP.mkdirs();
                }
                try {
                    FileUtils.forceDelete(DUMP);
                } catch (IOException ignored) {
                }
            } else {
                DUMP = null;
            }
            SWITCH_TABLE_FIXER = (Function<byte[], byte[]>) Class.forName("io.izzel.arclight.boot.asm.SwitchTableFixer").getField("INSTANCE").get(null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private final JarMapping toNmsMapping;
    private final JarMapping toBukkitMapping;
    public final InheritanceMap inheritanceMap;
    private final List<PluginTransformer> transformerList = new ArrayList<>();
    private final JarRemapper toBukkitRemapper;
    private final JarRemapper toNmsRemapper;
    private final List<PluginPatcher> patchers;

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
        this.patchers = ArclightPluginPatcher.load(this.transformerList);
        toBukkitMapping.setFallbackInheritanceProvider(GlobalClassRepo.inheritanceProvider());
        this.toBukkitRemapper = new LenientJarRemapper(toBukkitMapping);
        this.toNmsRemapper = new LenientJarRemapper(toNmsMapping);
        RemapSourceHandler.register();
    }

    public static ClassLoaderRemapper createClassLoaderRemapper(ClassLoader classLoader) {
        return new ClassLoaderRemapper(INSTANCE.copyOf(INSTANCE.toNmsMapping), INSTANCE.copyOf(INSTANCE.toBukkitMapping), classLoader);
    }

    public static JarRemapper getResourceMapper() {
        return INSTANCE.toBukkitRemapper;
    }

    public static JarRemapper getNmsMapper() {
        return INSTANCE.toNmsRemapper;
    }

    public List<PluginTransformer> getTransformerList() {
        return transformerList;
    }

    public List<PluginPatcher> getPatchers() {
        return patchers;
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
