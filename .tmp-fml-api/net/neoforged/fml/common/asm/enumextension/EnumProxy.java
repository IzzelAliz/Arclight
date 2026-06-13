/*
 * Copyright (c) NeoForged and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.fml.common.asm.enumextension;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import org.jetbrains.annotations.ApiStatus;

/**
 * Combined list of parameters used to create a new enum constant and mutable holder of the created enum constant
 */
public final class EnumProxy<T extends Enum<T> & IExtensibleEnum> {
    private final Class<T> enumClass;
    private final List<Object> parameters;
    private volatile T enumValue;

    public EnumProxy(Class<T> enumClass, Object... parameters) {
        this(enumClass, Arrays.asList(parameters));
    }

    public EnumProxy(Class<T> enumClass, List<Object> parameters) {
        this.enumClass = enumClass;
        this.parameters = parameters;
    }

    @ApiStatus.Internal
    public Object getParameter(int idx) {
        return parameters.get(idx);
    }

    /**
     * {@return the created enum constant, ensuring the containing enum is loaded if the value was not set yet}
     */
    public T getValue() {
        if (enumValue == null) {
            try {
                Class.forName(enumClass.getName(), true, enumClass.getClassLoader());
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
        return Objects.requireNonNull(enumValue, "Enum not initialized. Did you forget to configure the field holding this proxy as a parameter in the enum extension config file?");
    }

    @ApiStatus.Internal
    public void setValue(T value) {
        this.enumValue = value;
    }
}
