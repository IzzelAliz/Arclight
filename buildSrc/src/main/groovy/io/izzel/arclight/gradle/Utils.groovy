package io.izzel.arclight.gradle

import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.function.Consumer

class Utils {

    static void download(String url, File dist) {
        dist.parentFile.mkdirs()
        def con = new URL(url).openConnection()
        def stream = con.getInputStream()
        Files.copy(stream, dist.toPath(), StandardCopyOption.REPLACE_EXISTING)
        stream.close()
    }

    static <T extends AutoCloseable> void using(T closeable, Consumer<T> consumer) {
        try {
            consumer.accept(closeable)
        } finally {
            closeable.close()
        }
    }

    static void write(InputStream i, OutputStream o) {
        def buf = new byte[1024]
        def len = 0
        while ((len = i.read(buf)) > 0) {
            o.write(buf, 0, len)
        }
    }
}
