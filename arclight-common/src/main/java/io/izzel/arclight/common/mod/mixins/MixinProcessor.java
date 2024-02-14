package io.izzel.arclight.common.mod.mixins;

import org.objectweb.asm.tree.ClassNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

public interface MixinProcessor {

    Logger LOGGER = LoggerFactory.getLogger("MixinProcessor");

    void accept(String className, ClassNode classNode, IMixinInfo mixinInfo);
}
