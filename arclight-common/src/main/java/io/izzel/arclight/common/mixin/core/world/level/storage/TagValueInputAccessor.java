package io.izzel.arclight.common.mixin.core.world.level.storage;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.storage.TagValueInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(TagValueInput.class)
public interface TagValueInputAccessor {

    @Accessor("input")
    CompoundTag arclight$getInput();
}
