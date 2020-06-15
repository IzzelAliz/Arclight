package io.izzel.arclight.impl.mixin.v1_14.core.world;

import net.minecraft.world.World;
import net.minecraft.world.border.WorldBorder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(World.class)
public abstract class WorldMixin_1_14 {

    // @formatter:off
    @Shadow public abstract WorldBorder getWorldBorder();
    // @formatter:on
}
