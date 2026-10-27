package io.izzel.arclight.common.mixin.core.server;

import net.minecraft.core.RegistryAccess;
import net.minecraft.server.RegistryLayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(RegistryLayer.class)
public interface RegistryLayerAccessor {
    @Accessor("STATIC_ACCESS")
    static RegistryAccess.Frozen arclight$staticAccess() {
        throw new AssertionError();
    }
}
