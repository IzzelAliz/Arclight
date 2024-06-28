package io.izzel.arclight.neoforge.mixin.core.world.entity;

import io.izzel.arclight.common.bridge.core.entity.EntityBridge;
import io.izzel.arclight.common.bridge.core.entity.LivingEntityBridge;
import io.izzel.tools.product.Product;
import io.izzel.tools.product.Product4;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.extensions.IEntityExtension;
import net.neoforged.neoforge.entity.PartEntity;
import net.neoforged.neoforge.event.EventHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Collection;

@Mixin(Entity.class)
public abstract class EntityMixin_NeoForge implements EntityBridge, IEntityExtension {

    // @formatter:off
    @Shadow public abstract Level level();
    @Shadow public abstract boolean isRemoved();
    @Shadow private float yRot;
    @Shadow private float xRot;
    @Shadow public abstract float getXRot();
    @Shadow public abstract void moveTo(double d, double e, double f, float g, float h);
    @Shadow public abstract void setDeltaMovement(Vec3 vec3);
    @Shadow public abstract void unRide();
    @Shadow public abstract float getYRot();
    @Shadow public abstract EntityType<?> getType();
    @Shadow protected abstract void removeAfterChangingDimensions();
    @Shadow(remap = false) public abstract Collection<ItemEntity> captureDrops(Collection<ItemEntity> par1);
    @Shadow(remap = false) public abstract void revive();
    @Shadow public abstract Vec3 position();
    @Shadow public abstract int getId();
    @Shadow public abstract void discard();
    @Shadow public abstract double getX();
    @Shadow public abstract double getY(double d);
    @Shadow public abstract double getZ();
    // @formatter:on

    @Override
    public void bridge$revive() {
        this.revive();
    }

    @Redirect(method = "updateFluidHeightAndDoFluidPushing()V", remap = false, at = @At(value = "INVOKE", remap = true, target="Lnet/minecraft/world/level/material/FluidState;getFlow(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/phys/Vec3;"))
    private Vec3 arclight$setLava(FluidState instance, BlockGetter level, BlockPos pos) {
        if (instance.getType().is(FluidTags.LAVA)) {
            this.bridge$setLastLavaContact(pos.immutable());
        }
        return instance.getFlow(level, pos);
    }

    @Redirect(method = "spawnAtLocation(Lnet/minecraft/world/item/ItemStack;F)Lnet/minecraft/world/entity/item/ItemEntity;", at = @At(value = "INVOKE", remap = false, ordinal = 0, target = "Lnet/minecraft/world/entity/Entity;captureDrops()Ljava/util/Collection;"))
    public Collection<ItemEntity> arclight$forceDrops(Entity entity) {
        Collection<ItemEntity> drops = entity.captureDrops();
        if (this instanceof LivingEntityBridge && ((LivingEntityBridge) this).bridge$isForceDrops()) {
            drops = null;
        }
        return drops;
    }

    @Override
    public boolean bridge$forge$isPartEntity() {
        return (Object) this instanceof PartEntity<?>;
    }

    @Override
    public Entity bridge$forge$getParent() {
        return ((PartEntity<?>) (Object) this).getParent();
    }

    @Override
    public Entity[] bridge$forge$getParts() {
        return this.getParts();
    }

    @Override
    public Product4<Boolean, Double, Double, Double> bridge$onEntityTeleportCommand(double x, double y, double z) {
        var event = EventHooks.onEntityTeleportCommand((Entity) (Object) this, x, y, z);
        return Product.of(event.isCanceled(), event.getTargetX(), event.getTargetY(), event.getTargetZ());
    }
}
