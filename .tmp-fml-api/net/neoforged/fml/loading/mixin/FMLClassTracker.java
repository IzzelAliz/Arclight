/*
 * Copyright (c) NeoForged and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.fml.loading.mixin;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.spongepowered.asm.service.IClassTracker;

/**
 * Tracks invalid (unloadable) classes so we can throw an exception inside the
 * TCL and class load events so we can report when classes were loaded before
 * we could transform them
 */
class FMLClassTracker implements IClassTracker {
    private final Set<String> invalidClasses = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final Set<String> loadedClasses = Collections.newSetFromMap(new ConcurrentHashMap<>());

    @Override
    public void registerInvalidClass(String className) {
        this.invalidClasses.add(className);
    }

    boolean isInvalidClass(String className) {
        return this.invalidClasses.contains(className);
    }

    @Override
    public boolean isClassLoaded(String className) {
        return this.loadedClasses.contains(className);
    }

    void addLoadedClass(String className) {
        this.loadedClasses.add(className);
    }

    @Override
    public String getClassRestrictions(String className) {
        return "";
    }
}
