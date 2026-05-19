package io.izzel.arclight.neoforge.mod;

import com.google.common.graph.Graph;
import com.google.common.graph.Graphs;
import io.izzel.arclight.api.Unsafe;
import io.izzel.arclight.common.mod.ArclightCommon;
import net.neoforged.fml.ModList;
import org.objectweb.asm.ClassReader;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Set;

public class NeoForgeCommonImpl implements ArclightCommon.Api {

    private static final MethodHandle MH_TRANSFORM;

    static {
        try {
            ClassLoader classLoader = NeoForgeCommonImpl.class.getClassLoader();
            Class<?> transformingClassLoader = Class.forName("cpw.mods.modlauncher.TransformingClassLoader");
            Field classTransformer = transformingClassLoader.getDeclaredField("classTransformer");
            classTransformer.setAccessible(true);
            Object transformer = classTransformer.get(classLoader);
            Method transform = transformer.getClass().getDeclaredMethod("transform", byte[].class, String.class, String.class);
            MH_TRANSFORM = Unsafe.lookup().unreflect(transform).bindTo(transformer);
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
        return ModList.get() != null && ModList.get().isLoaded(modid);
    }

    @Override
    public <T> Set<T> guavaReachableNodes(Graph<T> graph, T node) {
        return Graphs.reachableNodes(graph, node);
    }
}
