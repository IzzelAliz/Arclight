package io.izzel.arclight.common.mixin.core.world;

import io.izzel.arclight.common.bridge.world.IWorldBridge;
import net.minecraft.world.IServerWorld;
import net.minecraft.world.server.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(IServerWorld.class)
public interface IServerWorldMixin extends IWorldBridge {

    // @formatter:off
    @Shadow ServerWorld getWorld();
    // @formatter:on

    @Override
    default ServerWorld bridge$getMinecraftWorld() {
        return this.getWorld();
    }
}
