package io.izzel.arclight.common.mod.mixins.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.CLASS)
@Target(ElementType.METHOD)
public @interface ShadowConstructor {

    @Retention(RetentionPolicy.CLASS)
    @Target(ElementType.METHOD)
    @interface Super {
    }
}
