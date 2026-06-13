/*
 * Copyright (c) NeoForged and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.fml.loading.mixin;

import net.neoforged.fml.common.asm.enumextension.IExtensibleEnum;
import org.objectweb.asm.Type;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.mixin.transformer.ClassInfo;
import org.spongepowered.asm.mixin.transformer.throwables.InvalidMixinException;
import org.spongepowered.asm.service.IFeatureValidator;

class FMLMixinFeatureValidator implements IFeatureValidator {
    private static final String I_EXTENSIBLE_ENUM = Type.getInternalName(IExtensibleEnum.class);

    @Override
    public void validateEnumExtension(IMixinInfo mixin, ClassInfo targetClass) throws InvalidMixinException {
        if (targetClass.findSuperClass(I_EXTENSIBLE_ENUM, ClassInfo.Traversal.ALL, true) != null) {
            throw new InvalidMixinException(
                    mixin,
                    ("Mixin Enum Extensions are not supported on target %s because it is an IExtensibleEnum, which " +
                            "should be extended as described here " +
                            "https://docs.neoforged.net/docs/advanced/extensibleenums/").formatted(targetClass));
        }
    }
}
