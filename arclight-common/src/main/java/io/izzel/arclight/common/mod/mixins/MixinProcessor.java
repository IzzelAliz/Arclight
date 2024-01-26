package io.izzel.arclight.common.mod.mixins;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

public interface MixinProcessor {

    void accept(String className, ClassNode classNode, IMixinInfo mixinInfo);
}
