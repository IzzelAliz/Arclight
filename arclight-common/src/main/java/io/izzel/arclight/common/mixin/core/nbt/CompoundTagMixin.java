package io.izzel.arclight.common.mixin.core.nbt;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import javax.annotation.Nullable;
import java.util.Map;

@Mixin(CompoundTag.class)
public class CompoundTagMixin {

    @Shadow @Final private Map<String, Tag> tags;

    /**
     * @author IzzelAliz
     * @reason drop forge patch
     */
    @Overwrite
    @Nullable
    public Tag put(String p_128366_, Tag p_128367_) {
        // if (p_128367_ == null) throw new IllegalArgumentException("Invalid null NBT value with key " + p_128366_);
        return this.tags.put(p_128366_, p_128367_);
    }
}
