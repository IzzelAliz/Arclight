package io.izzel.arclight.common.mod.util.remapper.generated;

import com.google.common.io.ByteStreams;
import io.izzel.arclight.common.mod.util.remapper.ArclightRemapper;
import io.izzel.arclight.common.mod.util.remapper.ClassLoaderRemapper;
import io.izzel.arclight.common.mod.util.remapper.RemappingClassLoader;
import io.izzel.tools.product.Product2;

import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.net.URLStreamHandlerFactory;
import java.security.CodeSource;
import java.util.concurrent.Callable;
import java.util.jar.Manifest;

public class RemappingURLClassLoader extends URLClassLoader implements RemappingClassLoader {

    static {
        ClassLoader.registerAsParallelCapable();
    }

    public RemappingURLClassLoader(URL[] urls, ClassLoader parent) {
        super(urls, RemappingClassLoader.asTransforming(parent));
    }

    public RemappingURLClassLoader(URL[] urls) {
        super(urls, RemappingClassLoader.asTransforming(null));
    }

    public RemappingURLClassLoader(URL[] urls, ClassLoader parent, URLStreamHandlerFactory factory) {
        super(urls, RemappingClassLoader.asTransforming(parent), factory);
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        Class<?> result = null;
        String path = name.replace('.', '/').concat(".class");
        URL resource = this.getResource(path);
        if (resource != null) {
            URLConnection connection;
            Callable<byte[]> byteSource;
            Manifest manifest;
            try {
                connection = resource.openConnection();
                connection.connect();
                if (connection instanceof JarURLConnection && ((JarURLConnection) connection).getManifest() != null) {
                    manifest = ((JarURLConnection) connection).getManifest();
                } else {
                    manifest = null;
                }
                byteSource = () -> {
                    try (InputStream is = connection.getInputStream()) {
                        byte[] classBytes = ByteStreams.toByteArray(is);
                        classBytes = ArclightRemapper.SWITCH_TABLE_FIXER.apply(classBytes);
                        return classBytes;
                    }
                };
            } catch (IOException e) {
                throw new ClassNotFoundException(name, e);
            }

            Product2<byte[], CodeSource> classBytes = this.getRemapper().remapClass(name, byteSource, connection);

            int i = name.lastIndexOf('.');
            if (i != -1) {
                String pkgName = name.substring(0, i);
                if (getPackage(pkgName) == null) {
                    if (manifest != null) {
                        this.definePackage(pkgName, manifest, ((JarURLConnection) connection).getJarFileURL());
                    } else {
                        this.definePackage(pkgName, null, null, null, null, null, null, null);
                    }
                }
            }
            result = this.defineClass(name, classBytes._1, 0, classBytes._1.length, classBytes._2);
        }
        if (result == null) {
            throw new ClassNotFoundException(name);
        }
        return result;
    }

    private ClassLoaderRemapper remapper;

    @Override
    public ClassLoaderRemapper getRemapper() {
        if (remapper == null) {
            remapper = ArclightRemapper.createClassLoaderRemapper(this);
        }
        return remapper;
    }
}
