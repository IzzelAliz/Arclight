package io.izzel.arclight.common.mixin.core.world.level;

import io.izzel.arclight.common.bridge.core.world.level.LevelAccessorBridge;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelAccessor;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(LevelAccessor.class)
public interface LevelAccessorMixin extends LevelAccessorBridge {

    default ServerLevel getMinecraftWorld() {
        return this.bridge$getMinecraftWorld();
    }
}
