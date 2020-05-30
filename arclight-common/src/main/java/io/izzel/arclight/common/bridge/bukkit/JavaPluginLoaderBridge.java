package io.izzel.arclight.common.bridge.bukkit;


import java.net.URLClassLoader;
import java.util.List;

public interface JavaPluginLoaderBridge {

    List<URLClassLoader> bridge$getLoaders();

    Class<?> bridge$getClassByName(final String name);

    void bridge$setClass(final String name, final Class<?> clazz);
}
