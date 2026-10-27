package io.izzel.arclight.common.mixin.core.world.level.storage;

import io.izzel.arclight.common.mod.mixins.annotation.TransformAccess;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.TagValueOutput;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(TagValueOutput.class)
public class TagValueOutputMixin {

    @TransformAccess(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC)
    private static TagValueOutput createWrappingWithContext(ProblemReporter reporter, HolderLookup.Provider lookup, CompoundTag output) {
        return new TagValueOutput(reporter, lookup.createSerializationContext(NbtOps.INSTANCE), output);
    }
}
