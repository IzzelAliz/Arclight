/*
 * Copyright (c) NeoForged and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.fml.classloading;

import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReference;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public final class JarContentsModuleFinder implements ModuleFinder {
    private final Map<String, ModuleReference> jars;

    public JarContentsModuleFinder(Collection<JarContentsModule> jars) {
        this.jars = jars.stream()
                .collect(Collectors.toMap(
                        JarContentsModule::moduleName,
                        sj -> new JarContentsModuleReference(
                                sj.moduleDescriptor(),
                                sj.contents())));
    }

    @Override
    public Optional<ModuleReference> find(String name) {
        return Optional.ofNullable(jars.get(name));
    }

    @Override
    public Set<ModuleReference> findAll() {
        return Set.copyOf(jars.values());
    }
}
