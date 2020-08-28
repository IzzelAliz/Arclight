package io.izzel.arclight.common.mixin.core.world.storage;

import net.minecraft.world.storage.DerivedWorldInfo;
import net.minecraft.world.storage.IServerWorldInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(DerivedWorldInfo.class)
public interface DerivedWorldInfoAccessor {

    @Accessor("delegate")
    IServerWorldInfo bridge$getDelegate();
}
