/*
 * Copyright (c) NeoForged and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.fml.loading.mixin;

import org.spongepowered.asm.service.IAdviceProvider;

class FMLMixinAdviceProvider implements IAdviceProvider {
    @Override
    public String higherCompatibilityNeeded(int requiredCompatibility, String requiredCompatibilityString) {
        return "Set `behaviorVersion = \"%s\"` or higher on your mixin config in your neoforge.mods.toml"
                .formatted(requiredCompatibilityString);
    }
}
