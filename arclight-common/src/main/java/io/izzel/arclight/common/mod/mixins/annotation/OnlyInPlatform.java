package io.izzel.arclight.common.mod.mixins.annotation;

import io.izzel.arclight.api.ArclightPlatform;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.CLASS)
public @interface OnlyInPlatform {

    ArclightPlatform[] value();
}
