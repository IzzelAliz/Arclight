/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.fml;

import net.neoforged.bus.api.IEventBus;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public interface IBindingsProvider {
    IEventBus getGameBus();
}
