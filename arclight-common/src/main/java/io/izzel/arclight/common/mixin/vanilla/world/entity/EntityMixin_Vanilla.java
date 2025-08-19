package io.izzel.arclight.common.mixin.vanilla.world.entity;

import io.izzel.arclight.common.bridge.core.entity.EntityBridge;
import io.izzel.arclight.common.bridge.core.entity.InternalEntityBridge;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.util.ArrayList;
import java.util.List;

@Mixin(Entity.class)
public abstract class EntityMixin_Vanilla implements InternalEntityBridge, EntityBridge {

    // @formatter:off
    @Shadow public abstract int getId();
    @Shadow public abstract Vec3 position();
    @Shadow public abstract void discard();
    @Shadow public abstract Level level();
    @Shadow public abstract EntityType<?> getType();
    @Shadow public abstract double getX();
    @Shadow public abstract double getY();
    @Shadow public abstract double getZ();
    // @formatter:on

    @Unique private List<ItemEntity> arclight$capturedDrops;

    @Unique
    public void arclight$startCaptureDrops() {
        arclight$capturedDrops = new ArrayList<>();
    }

    @Unique
    public boolean arclight$captureDrop(ItemEntity itemEntity) {
        if (arclight$capturedDrops != null) {
            arclight$capturedDrops.add(itemEntity);
            return true;
        }
        return false;
    }

    @Unique
    public List<ItemEntity> arclight$finishCaptureDrops() {
        final var drops = arclight$capturedDrops;
        arclight$capturedDrops = null;
        return drops;
    }

    @Decorate(method = "updateFluidHeightAndDoFluidPushing", require = 0, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/material/FluidState;getFlow(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/phys/Vec3;"))
    private Vec3 arclight$setLava(FluidState fluid, BlockGetter level, BlockPos pos) throws Throwable {
        if (fluid.getType().is(FluidTags.LAVA)) {
            bridge$setLastLavaContact(pos.immutable());
        }
        return (Vec3) DecorationOps.callsite().invoke(fluid, level, pos);
    }

    @Decorate(method = "spawnAtLocation(Lnet/minecraft/world/item/ItemStack;F)Lnet/minecraft/world/entity/item/ItemEntity;", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"))
    private boolean arclight$captureEntityDrops(Level instance, Entity entity) throws Throwable {
        if (!bridge$isForceDrops() && arclight$captureDrop((ItemEntity) entity)) {
            return true;
        }
        return (boolean) DecorationOps.callsite().invoke(instance, entity);
    }
}
