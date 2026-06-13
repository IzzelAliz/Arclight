/*
 * Copyright (c) NeoForged and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.fml.loading.mixin;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.spongepowered.asm.service.IGlobalPropertyService;
import org.spongepowered.asm.service.IPropertyKey;

public class FMLMixinGlobalProperties implements IGlobalPropertyService {
    private static final Map<String, Object> PROPERTIES = new HashMap<>();

    @Override
    public IPropertyKey resolveKey(String name) {
        return new StringKey(name);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getProperty(IPropertyKey key) {
        synchronized (PROPERTIES) {
            return (T) PROPERTIES.get(getKeyName(key));
        }
    }

    @Override
    public void setProperty(IPropertyKey key, Object value) {
        synchronized (PROPERTIES) {
            PROPERTIES.put(getKeyName(key), value);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getProperty(IPropertyKey key, T defaultValue) {
        synchronized (PROPERTIES) {
            return (T) PROPERTIES.getOrDefault(getKeyName(key), defaultValue);
        }
    }

    @Override
    public String getPropertyString(IPropertyKey key, String defaultValue) {
        return Objects.requireNonNullElse((String) PROPERTIES.get(getKeyName(key)), defaultValue);
    }

    private String getKeyName(IPropertyKey key) {
        return ((StringKey) key).name();
    }

    record StringKey(String name) implements IPropertyKey {}
}
