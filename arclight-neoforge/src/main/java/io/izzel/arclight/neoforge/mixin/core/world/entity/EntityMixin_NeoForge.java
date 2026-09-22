package io.izzel.arclight.neoforge.mixin.core.world.entity;

import io.izzel.arclight.common.bridge.core.entity.EntityBridge;
import io.izzel.arclight.common.bridge.core.world.damagesource.DamageSourceBridge;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import io.izzel.tools.product.Product;
import io.izzel.tools.product.Product4;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.extensions.IEntityExtension;
import net.neoforged.neoforge.entity.PartEntity;
import net.neoforged.neoforge.event.EventHooks;
import org.bukkit.Bukkit;
import org.bukkit.entity.Hanging;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Collection;

@Mixin(Entity.class)
public abstract class EntityMixin_NeoForge implements EntityBridge, IEntityExtension {

    @Shadow public abstract Level level();
    @Shadow public abstract void discard();
    @Shadow public abstract double getX();
    @Shadow public abstract double getY(double offset);
    @Shadow public abstract double getZ();
    @Shadow public abstract boolean fireImmune();
    @Shadow(remap = false) public abstract void revive();

    @Override
    public void bridge$revive() {
        this.revive();
    }

    @Redirect(method = "updateFluidHeightAndDoFluidPushing(Z)V", remap = false, at = @At(value = "INVOKE", remap = true, target="Lnet/minecraft/world/level/material/FluidState;getFlow(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/phys/Vec3;"))
    private Vec3 arclight$setLava(FluidState instance, BlockGetter level, BlockPos pos) {
        if (instance.getType().is(FluidTags.LAVA)) {
            this.bridge$setLastLavaContact(pos.immutable());
        }
        return instance.getFlow(level, pos);
    }

    @Redirect(method = "spawnAtLocation(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/entity/item/ItemEntity;", at = @At(value = "INVOKE", remap = false, ordinal = 0, target = "Lnet/minecraft/world/entity/Entity;captureDrops()Ljava/util/Collection;"))
    public Collection<ItemEntity> arclight$forceDrops(Entity entity) {
        Collection<ItemEntity> drops = entity.captureDrops();
        if (this.bridge$isForceDrops()) {
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

    @Decorate(method = "thunderHit", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)V"))
    private void arclight$lightningDamage(Entity entity, DamageSource source, float amount, ServerLevel level, LightningBolt lightning) throws Throwable {
        var victim = this.bridge$getBukkitEntity();
        if (victim instanceof Hanging hanging) {
            var event = new HangingBreakByEntityEvent(hanging, ((EntityBridge) lightning).bridge$getBukkitEntity());
            Bukkit.getPluginManager().callEvent(event);
            if (event.isCancelled()) return;
        }
        if (this.fireImmune()) return;
        DecorationOps.callsite().invoke(entity, ((DamageSourceBridge) source).bridge$customCausingEntity(lightning), amount);
    }

    @Override
    public Product4<Boolean, Double, Double, Double> bridge$onEntityTeleportCommand(double x, double y, double z) {
        var event = EventHooks.onEntityTeleportCommand((Entity) (Object) this, x, y, z);
        return Product.of(event.isCanceled(), event.getTargetX(), event.getTargetY(), event.getTargetZ());
    }
}
