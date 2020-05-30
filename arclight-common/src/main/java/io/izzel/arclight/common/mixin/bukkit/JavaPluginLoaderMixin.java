package io.izzel.arclight.common.mixin.bukkit;

import io.izzel.arclight.common.bridge.bukkit.JavaPluginLoaderBridge;
import org.bukkit.plugin.java.JavaPluginLoader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.net.URLClassLoader;
import java.util.List;

@Mixin(value = JavaPluginLoader.class, remap = false)
public interface JavaPluginLoaderMixin extends JavaPluginLoaderBridge {

    // @formatter:off
    @Invoker("getClassByName") Class<?> bridge$getClassByName(final String name);
    @Invoker("setClass") void bridge$setClass(final String name, final Class<?> clazz);
    @Accessor("loaders") List<URLClassLoader> bridge$getLoaders();
    // @formatter:on
}
