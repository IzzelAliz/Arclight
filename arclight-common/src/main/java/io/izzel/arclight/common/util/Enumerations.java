package io.izzel.arclight.common.util;

import io.izzel.arclight.api.Unsafe;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.function.Function;

public class Enumerations {

    public static <A, B> Enumeration<B> transform(Enumeration<A> enumeration, Function<A, B> mapper) {
        return new Enumeration<B>() {
            @Override
            public boolean hasMoreElements() {
                return enumeration.hasMoreElements();
            }

            @Override
            public B nextElement() {
                return mapper.apply(enumeration.nextElement());
            }
        };
    }

    public static Enumeration<URL> remapped(Enumeration<URL> enumeration) {
        return transform(enumeration, url -> {
            try {
                return new URL("remap:" + url);
            } catch (MalformedURLException e) {
                Unsafe.throwException(e);
                return null;
            }
        });
    }
}
