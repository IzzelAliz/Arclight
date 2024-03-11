package io.izzel.arclight.common.mod.mixins.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.CLASS)
public @interface LoadIfMod {

    String[] modid();

    ModCondition condition();

    enum ModCondition {
        ABSENT,
        PRESENT
    }
}
