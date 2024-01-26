package io.izzel.arclight.common.mod.util.remapper;

public interface RemappingClassLoader {

    ClassLoaderRemapper getRemapper();

    static ClassLoader asTransforming(ClassLoader classLoader) {
        if (classLoader == ClassLoader.getPlatformClassLoader() || classLoader == ClassLoader.getSystemClassLoader() || classLoader == null) {
            return RemappingClassLoader.class.getClassLoader();
        }
        return classLoader;
    }
}
