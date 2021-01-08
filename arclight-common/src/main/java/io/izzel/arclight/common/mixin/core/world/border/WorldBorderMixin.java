package io.izzel.arclight.common.mixin.core.world.border;

import io.izzel.arclight.common.bridge.world.border.WorldBorderBridge;
import net.minecraft.world.border.WorldBorder;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(WorldBorder.class)
public class WorldBorderMixin implements WorldBorderBridge {

    public World world;

    @Override
    public World bridge$getWorld() {
        return this.world;
    }

    @Override
    public void bridge$setWorld(World world) {
        this.world = world;
    }
}
