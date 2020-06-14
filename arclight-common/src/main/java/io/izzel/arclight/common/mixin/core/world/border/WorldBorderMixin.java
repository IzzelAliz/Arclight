package io.izzel.arclight.common.mixin.core.world.border;

import io.izzel.arclight.common.bridge.world.border.WorldBorderBridge;
import net.minecraft.world.border.WorldBorder;
import net.minecraft.world.server.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(WorldBorder.class)
public class WorldBorderMixin implements WorldBorderBridge {

    public ServerWorld world;

    @Override
    public ServerWorld bridge$getWorld() {
        return this.world;
    }

    @Override
    public void bridge$setWorld(ServerWorld world) {
        this.world = world;
    }
}
