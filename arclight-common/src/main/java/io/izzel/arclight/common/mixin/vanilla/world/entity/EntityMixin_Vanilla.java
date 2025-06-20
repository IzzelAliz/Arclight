package io.izzel.arclight.common.mixin.vanilla.world.entity;

import io.izzel.arclight.common.bridge.core.entity.EntityBridge;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Entity.class)
public abstract class EntityMixin_Vanilla implements EntityBridge {

    // @formatter:off
    @Shadow public abstract int getId();
    @Shadow public abstract Vec3 position();
    @Shadow public abstract void unRide();
    @Shadow public abstract void discard();
    @Shadow public abstract Level level();
    // @formatter:on

    @Shadow public abstract EntityType<?> getType();

    @Shadow public int invulnerableTime;

    @Decorate(method = "updateFluidHeightAndDoFluidPushing", require = 0, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/material/FluidState;getFlow(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/phys/Vec3;"))
    private Vec3 arclight$setLava(FluidState fluid, BlockGetter level, BlockPos pos) throws Throwable {
        if (fluid.getType().is(FluidTags.LAVA)) {
            bridge$setLastLavaContact(pos.immutable());
        }
        return (Vec3) DecorationOps.callsite().invoke(fluid, level, pos);
    }
}
