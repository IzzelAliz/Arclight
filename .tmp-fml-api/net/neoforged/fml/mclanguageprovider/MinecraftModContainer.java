/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.fml.mclanguageprovider;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforgespi.language.IModInfo;
import org.jetbrains.annotations.Nullable;

public class MinecraftModContainer extends ModContainer {
    public MinecraftModContainer(IModInfo info) {
        super(info);
    }

    @Override
    public @Nullable IEventBus getEventBus() {
        return null;
    }
}
