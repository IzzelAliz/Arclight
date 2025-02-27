package io.izzel.arclight.gradle.util;

import lombok.SneakyThrows;

import java.net.URI;
import java.net.URL;

public class URLHelper {
    @SneakyThrows
    public static URL of(String url) {
        return URI.create(url).toURL();
    }

    @SneakyThrows
    public static URL of(URI uri) {
        return uri.toURL();
    }
}
