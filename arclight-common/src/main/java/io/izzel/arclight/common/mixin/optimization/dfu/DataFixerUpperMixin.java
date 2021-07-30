package io.izzel.arclight.common.mixin.optimization.dfu;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFixerUpper;
import com.mojang.serialization.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(value = DataFixerUpper.class, remap = false)
public abstract class DataFixerUpperMixin {

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public <T> Dynamic<T> update(DSL.TypeReference type, Dynamic<T> input, int version, int newVersion) {
        return input;
    }
}
