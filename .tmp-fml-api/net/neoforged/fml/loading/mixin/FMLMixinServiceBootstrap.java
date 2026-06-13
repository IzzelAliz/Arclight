/*
 * Copyright (c) NeoForged and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.fml.loading.mixin;

import org.spongepowered.asm.service.IMixinServiceBootstrap;

public class FMLMixinServiceBootstrap implements IMixinServiceBootstrap {
    @Override
    public String getName() {
        return "fml";
    }

    @Override
    public String getServiceClassName() {
        return FMLMixinService.class.getName();
    }

    @Override
    public void bootstrap() {}
}
