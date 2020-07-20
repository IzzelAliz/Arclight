package io.izzel.arclight.common.mod.util.remapper;

import cpw.mods.modlauncher.api.ITransformingClassLoader;

public interface RemappingClassLoader {

    ClassLoaderRemapper getRemapper();

    static ClassLoader asTransforming(ClassLoader classLoader) {
        boolean found = false;
        while (classLoader != null) {
            if (classLoader instanceof ITransformingClassLoader || classLoader instanceof RemappingClassLoader) {
                found = true;
                break;
            } else {
                classLoader = classLoader.getParent();
            }
        }
        return found ? classLoader : RemappingClassLoader.class.getClassLoader();
    }
}
