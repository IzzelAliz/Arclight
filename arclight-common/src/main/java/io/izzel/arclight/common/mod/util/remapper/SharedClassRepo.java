package io.izzel.arclight.common.mod.util.remapper;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import io.izzel.arclight.common.bridge.bukkit.JavaPluginLoaderBridge;
import net.md_5.specialsource.repo.ClassRepo;
import org.bukkit.plugin.java.JavaPluginLoader;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.service.MixinService;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;

public class SharedClassRepo implements ClassRepo {

    private static SharedClassRepo classRepo;

    protected final JavaPluginLoaderBridge loader;
    private final Cache<String, ClassNode> cache = CacheBuilder.newBuilder().maximumSize(65536).build();

    protected SharedClassRepo(JavaPluginLoader loader) {
        this.loader = ((JavaPluginLoaderBridge) (Object) loader);
    }

    @Override
    public ClassNode findClass(String internalName) {
        ClassNode ret = cache.getIfPresent(internalName);
        if (ret != null) {
            return ret;
        }
        try {
            ret = MixinService.getService().getBytecodeProvider().getClassNode(internalName);
        } catch (Exception ignored) {
        }
        if (ret == null) {
            ret = findPlugins(internalName + ".class");
        }
        if (ret != null) {
            cache.put(internalName, ret);
        }
        return ret;
    }

    private ClassNode findPlugins(String name) {
        for (URLClassLoader classLoader : loader.bridge$getLoaders()) {
            ClassNode node = findIn(classLoader, name);
            if (node != null) return node;
        }
        return null;
    }

    protected ClassNode findIn(URLClassLoader classLoader, String name) {
        URL url = HelperClassLoader.find(classLoader, name);
        if (url == null) return null;
        try {
            URLConnection connection = url.openConnection();
            try (InputStream inputStream = connection.getInputStream()) {
                ClassReader reader = new ClassReader(inputStream);
                ClassNode classNode = new ClassNode();
                reader.accept(classNode, ClassReader.SKIP_CODE);
                return classNode;
            }
        } catch (IOException ignored) {
        }
        return null;
    }

    public static ClassRepo get(JavaPluginLoader loader, URLClassLoader plugin) {
        if (classRepo == null) {
            synchronized (SharedClassRepo.class) {
                if (classRepo == null) {
                    classRepo = new SharedClassRepo(loader);
                }
            }
        }
        return new PluginClassRepo(classRepo, loader, plugin);
    }

    private static class HelperClassLoader extends URLClassLoader {

        public HelperClassLoader(URL[] urls) {
            super(urls);
        }

        static URL find(URLClassLoader classLoader, String resource) {
            return classLoader.findResource(resource); // search local
        }
    }
}
