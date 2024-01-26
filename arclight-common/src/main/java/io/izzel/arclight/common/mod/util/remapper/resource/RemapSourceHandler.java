package io.izzel.arclight.common.mod.util.remapper.resource;

import com.google.common.io.ByteStreams;
import io.izzel.arclight.api.Unsafe;
import io.izzel.arclight.common.mod.ArclightCommon;
import io.izzel.arclight.common.mod.util.remapper.ArclightRemapper;
import io.izzel.arclight.common.mod.util.remapper.GlobalClassRepo;
import org.objectweb.asm.ClassReader;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandle;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.util.Hashtable;

public class RemapSourceHandler extends URLStreamHandler {

    @Override
    protected URLConnection openConnection(URL u) throws IOException {
        return new RemapSourceConnection(new URL(u.getFile()));
    }

    private static class RemapSourceConnection extends URLConnection {

        private byte[] array;

        protected RemapSourceConnection(URL url) {
            super(url);
        }

        @Override
        public void connect() throws IOException {
            byte[] bytes = ByteStreams.toByteArray(url.openStream());
            String className = new ClassReader(bytes).getClassName();
            if (className.startsWith("net/minecraft/") || className.equals("com/mojang/brigadier/tree/CommandNode")) {
                bytes = ArclightCommon.api().platformRemapClass(bytes);
            }
            this.array = ArclightRemapper.getResourceMapper().remapClassFile(bytes, GlobalClassRepo.INSTANCE);
        }

        @Override
        public InputStream getInputStream() throws IOException {
            connect();
            if (this.array == null) {
                throw new FileNotFoundException(this.url.getFile());
            } else {
                return new ByteArrayInputStream(this.array);
            }
        }
    }

    @SuppressWarnings("unchecked")
    public static void register() {
        try {
            Unsafe.ensureClassInitialized(URL.class);
            MethodHandle getter = Unsafe.lookup().findStaticGetter(URL.class, "handlers", Hashtable.class);
            Hashtable<String, URLStreamHandler> handlers = (Hashtable<String, URLStreamHandler>) getter.invokeExact();
            handlers.put("remap", new RemapSourceHandler());
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }
}
