package io.izzel.arclight.common.mixin.v1_15.world;

import io.izzel.arclight.common.bridge.world.dimension.DimensionTypeBridge;
import net.minecraft.world.World;
import net.minecraft.world.dimension.Dimension;
import net.minecraft.world.dimension.DimensionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(World.class)
public class WorldMixin_1_14 {

    @Redirect(method = "isNightTime", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/dimension/Dimension;getType()Lnet/minecraft/world/dimension/DimensionType;"))
    private DimensionType arclight$nightTimeType(Dimension dimension) {
        return ((DimensionTypeBridge) dimension.getType()).bridge$getType();
    }
}
