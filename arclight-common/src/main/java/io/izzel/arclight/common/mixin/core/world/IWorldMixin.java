package io.izzel.arclight.common.mixin.core.world;

import io.izzel.arclight.common.bridge.core.world.IWorldBridge;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelAccessor;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(LevelAccessor.class)
public interface IWorldMixin extends IWorldBridge {

    default ServerLevel getMinecraftWorld() {
        return this.bridge$getMinecraftWorld();
    }
}
