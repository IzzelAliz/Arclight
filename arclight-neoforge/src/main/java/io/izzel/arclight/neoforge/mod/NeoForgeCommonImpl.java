package io.izzel.arclight.neoforge.mod;

import cpw.mods.modlauncher.ClassTransformer;
import cpw.mods.modlauncher.TransformingClassLoader;
import io.izzel.arclight.api.Unsafe;
import io.izzel.arclight.common.mod.ArclightCommon;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLLoader;
import org.objectweb.asm.ClassReader;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class NeoForgeCommonImpl implements ArclightCommon.Api {

    private static final MethodHandle MH_TRANSFORM;

    static {
        try {
            ClassLoader classLoader = NeoForgeCommonImpl.class.getClassLoader();
            Field classTransformer = TransformingClassLoader.class.getDeclaredField("classTransformer");
            classTransformer.setAccessible(true);
            ClassTransformer tranformer = (ClassTransformer) classTransformer.get(classLoader);
            Method transform = tranformer.getClass().getDeclaredMethod("transform", byte[].class, String.class, String.class);
            MH_TRANSFORM = Unsafe.lookup().unreflect(transform).bindTo(tranformer);
        } catch (Throwable t) {
            throw new IllegalStateException("Unknown modlauncher version", t);
        }
    }

    @Override
    public byte[] platformRemapClass(byte[] cl) {
        String className = new ClassReader(cl).getClassName();
        try {
            return (byte[]) MH_TRANSFORM.invokeExact(cl, className.replace('/', '.'), "source");
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean isModLoaded(String modid) {
        return ModList.get() != null ? ModList.get().isLoaded(modid) : FMLLoader.getLoadingModList().getModFileById(modid) != null;
    }
}
