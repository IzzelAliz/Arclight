package io.izzel.arclight.common.mixin.core.world;

import io.izzel.arclight.common.bridge.world.IWorldBridge;
import net.minecraft.world.IWorld;
import net.minecraft.world.server.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(IWorld.class)
public interface IWorldMixin extends IWorldBridge {

    default ServerWorld getMinecraftWorld() {
        return this.bridge$getMinecraftWorld();
    }
}
