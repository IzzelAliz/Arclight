package io.izzel.arclight.gradle.runnable;

import io.izzel.arclight.gradle.util.URLHelper;
import lombok.SneakyThrows;

import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class FileDownloader implements Runnable {
    private final URL url;
    private final Path output;
    private final boolean replace;

    public FileDownloader(String url, File output) {
        this(URLHelper.of(url), output.toPath(), false);
    }

    public FileDownloader(String url, Path output) {
        this(URLHelper.of(url), output, false);
    }

    public FileDownloader(URL url, Path output) {
        this(url, output, false);
    }

    public FileDownloader(URL url, Path output, boolean replace) {
        this.url = url;
        this.output = output;
        this.replace = replace;
    }

    @Override
    @SneakyThrows
    public void run() {
        if (Files.exists(output)) {
            if (replace) {
                Files.deleteIfExists(output);
            } else {
                return;
            }
        }

        Files.createDirectories(output.getParent());

        var connection = url.openConnection();
        var stream = connection.getInputStream();
        Files.copy(stream, output, StandardCopyOption.REPLACE_EXISTING);
        stream.close();
    }
}
