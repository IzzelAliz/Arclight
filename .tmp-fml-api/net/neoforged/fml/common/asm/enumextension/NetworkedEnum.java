/*
 * Copyright (c) NeoForged and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.fml.common.asm.enumextension;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Specifies that the annotated enum is used in networking and must be checked for mismatches between the client and server
 * 
 * @see ExtensionInfo
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface NetworkedEnum {
    NetworkCheck value();

    enum NetworkCheck {
        /**
         * To be used for enums which are sent to the client, allowing connections to a vanilla server when the
         * enum is extended on the client but not to a vanilla client when the enum is extended on the server
         */
        CLIENTBOUND,
        /**
         * To be used for enums which are sent to the server, allowing connections to a vanilla client when the
         * enum is extended on the server but not to a vanilla server when the enum is extended on the client
         */
        SERVERBOUND,
        /**
         * To be used for enums which are sent in both directions, disallowing connections to either vanilla
         * counterpart when the enum is extended
         */
        BIDIRECTIONAL
    }
}
