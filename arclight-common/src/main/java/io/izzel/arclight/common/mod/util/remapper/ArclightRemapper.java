package io.izzel.arclight.common.mod.util.remapper;

import io.izzel.arclight.api.PluginPatcher;
import io.izzel.arclight.common.mod.util.remapper.patcher.ArclightPluginPatcher;
import io.izzel.arclight.common.mod.util.remapper.patcher.PluginLoggerTransformer;
import io.izzel.arclight.common.mod.util.remapper.resource.RemapSourceHandler;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

@SuppressWarnings("unchecked")
public class ArclightRemapper {

    public static final ArclightRemapper INSTANCE;
    public static final File DUMP;
    public static final Function<byte[], byte[]> SWITCH_TABLE_FIXER;

    static {
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

    private final List<PluginTransformer> transformerList = new ArrayList<>();
    private final List<PluginPatcher> patchers;

    public ArclightRemapper() {
        this.transformerList.add(ArclightRedirectAdapter.INSTANCE);
        this.transformerList.add(ClassLoaderAdapter.INSTANCE);
        if (!(java.util.logging.LogManager.getLogManager() instanceof org.apache.logging.log4j.jul.LogManager)) {
            this.transformerList.add(new PluginLoggerTransformer());
        }
        this.patchers = ArclightPluginPatcher.load(this.transformerList);
        RemapSourceHandler.register();
    }

    public static ClassLoaderRemapper createClassLoaderRemapper(ClassLoader classLoader) {
        return new ClassLoaderRemapper(classLoader);
    }

    public List<PluginTransformer> getTransformerList() {
        return transformerList;
    }

    public List<PluginPatcher> getPatchers() {
        return patchers;
    }
}
