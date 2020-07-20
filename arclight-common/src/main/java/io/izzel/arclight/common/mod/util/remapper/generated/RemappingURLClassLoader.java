package io.izzel.arclight.common.mod.util.remapper.generated;

import com.google.common.io.ByteStreams;
import io.izzel.arclight.common.mod.util.remapper.ArclightRemapper;
import io.izzel.arclight.common.mod.util.remapper.ClassLoaderRemapper;
import io.izzel.arclight.common.mod.util.remapper.RemappingClassLoader;

import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.net.URLStreamHandlerFactory;
import java.security.CodeSource;

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
            try {
                URLConnection connection = resource.openConnection();
                byte[] bytes = getRemapper().remapClass(ByteStreams.toByteArray(connection.getInputStream()));
                int i = name.lastIndexOf('.');
                if (i != -1) {
                    String pkgName = name.substring(0, i);
                    if (getPackage(pkgName) == null) {
                        if (connection instanceof JarURLConnection && ((JarURLConnection) connection).getManifest() != null) {
                            this.definePackage(pkgName, ((JarURLConnection) connection).getManifest(), ((JarURLConnection) connection).getJarFileURL());
                        } else {
                            this.definePackage(pkgName, null, null, null, null, null, null, null);
                        }
                    }
                }
                CodeSource codeSource;
                if (connection instanceof JarURLConnection) {
                    codeSource = new CodeSource(((JarURLConnection) connection).getJarFileURL(), ((JarURLConnection) connection).getJarEntry().getCodeSigners());
                } else {
                    codeSource = null;
                }
                result = this.defineClass(name, bytes, 0, bytes.length, codeSource);
            } catch (IOException e) {
                throw new ClassNotFoundException(name, e);
            }
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
