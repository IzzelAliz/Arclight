package io.izzel.arclight.common.mixin.core.world.level.storage;

import io.izzel.arclight.common.mod.mixins.annotation.TransformAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.ValueInput;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(TagValueInput.class)
public abstract class TagValueInputMixin {
    @TransformAccess(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC)
    private static ValueInput createGlobal(ProblemReporter reporter, CompoundTag tag) {
        return TagValueInput.create(reporter, MinecraftServer.getServer().registryAccess(), tag);
    }
}
