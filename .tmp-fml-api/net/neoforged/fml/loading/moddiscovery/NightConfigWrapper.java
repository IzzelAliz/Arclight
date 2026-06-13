/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.fml.loading.moddiscovery;

import static java.util.Arrays.asList;

import com.electronwill.nightconfig.core.UnmodifiableConfig;
import com.electronwill.nightconfig.core.concurrent.ConcurrentConfig;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import net.neoforged.neoforgespi.language.IConfigurable;
import net.neoforged.neoforgespi.language.IModFileInfo;
import net.neoforged.neoforgespi.locating.InvalidModFileException;

public class NightConfigWrapper implements IConfigurable {
    private final UnmodifiableConfig config;
    private IModFileInfo file;

    public NightConfigWrapper(UnmodifiableConfig config) {
        if (config instanceof ConcurrentConfig) {
            throw new IllegalArgumentException("Cannot create a NightConfigWrapper with a ConcurrentConfig!");
        }
        this.config = config;
    }

    public NightConfigWrapper setFile(IModFileInfo file) {
        this.file = file;
        return this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Optional<T> getConfigElement(String... key) {
        var path = asList(key);
        return this.config.getOptional(path).map(value -> {
            if (value instanceof UnmodifiableConfig) {
                return (T) ((UnmodifiableConfig) value).valueMap();
            } else if (value instanceof ArrayList<?> al && al.size() > 0 && al.get(0) instanceof UnmodifiableConfig) {
                throw new InvalidModFileException("The configuration path " + path + " is invalid. I wasn't expecting a multi-object list - remove one of the [[ ]]", file);
            }
            return (T) value;
        });
    }

    @Override
    public List<? extends IConfigurable> getConfigList(String... key) {
        List<String> path = asList(key);
        if (this.config.contains(path) && !(this.config.get(path) instanceof Collection)) {
            throw new InvalidModFileException("The configuration path " + path + " is invalid. Expecting a collection!", file);
        }
        Collection<UnmodifiableConfig> nestedConfigs = this.config.getOrElse(path, ArrayList::new);
        return nestedConfigs.stream()
                .map(NightConfigWrapper::new)
                .map(cw -> cw.setFile(file))
                .collect(Collectors.toList());
    }
}
