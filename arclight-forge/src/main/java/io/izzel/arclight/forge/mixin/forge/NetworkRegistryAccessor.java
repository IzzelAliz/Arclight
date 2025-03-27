package io.izzel.arclight.forge.mixin.forge;

import net.minecraftforge.network.NetworkRegistry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = NetworkRegistry.class, remap = false)
public interface NetworkRegistryAccessor {
    @Accessor
    static void setLock(boolean value) {
        throw new AbstractMethodError();
    }
}
