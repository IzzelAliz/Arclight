/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.fml.common;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import net.neoforged.api.distmarker.Dist;

/**
 * This defines a Mod to FML.
 * <p>
 * Any class found with this annotation applied will be loaded as a mod entrypoint for the mod with the given {@linkplain #value() ID}. <br>
 * A mod loaded with the {@code javafml} language loader may have multiple entrypoints.
 * Entrypoints with the least {@link #depends} are run first.
 * Entrypoints for all {@link #dist}s are always run before entrypoints for a single {@link #dist}.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Mod {
    /**
     * The unique mod identifier for this mod.
     * <b>Required to be lowercased in the english locale for compatibility. Will be truncated to 64 characters long.</b>
     * <p>
     * This will be used to identify your mod for third parties (other mods), it will be used to identify your mod for registries such as block and item registries.
     * By default, you will have a resource domain that matches the modid. All these uses require that constraints are imposed on the format of the modid.
     */
    String value();

    /**
     * {@return the side to load this mod entrypoint on}
     */
    Dist[] dist() default { Dist.CLIENT, Dist.DEDICATED_SERVER };

    /**
     * A list of mod IDs which are all required to be present in order to load this mod entrypoint.
     */
    String[] depends() default {};
}
