package io.izzel.arclight.common.mixin.core.world.entity.vehicle;

import io.izzel.arclight.common.bridge.core.entity.EntityBridge;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.entity.vehicle.minecart.MinecartBehavior;
import net.minecraft.world.entity.vehicle.minecart.NewMinecartBehavior;
import org.bukkit.Bukkit;
import org.bukkit.entity.Vehicle;
import org.bukkit.event.vehicle.VehicleEntityCollisionEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(NewMinecartBehavior.class)
public abstract class NewMinecartBehaviorMixin extends MinecartBehavior {
    protected NewMinecartBehaviorMixin(AbstractMinecart minecart) {
        super(minecart);
    }

    @Decorate(method = "pickupEntities", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;startRiding(Lnet/minecraft/world/entity/Entity;)Z"))
    private boolean arclight$pickupCollision(Entity entity, Entity vehicle) throws Throwable {
        VehicleEntityCollisionEvent event = new VehicleEntityCollisionEvent((Vehicle) ((EntityBridge) this.minecart).bridge$getBukkitEntity(), ((EntityBridge) entity).bridge$getBukkitEntity());
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            throw DecorationOps.jumpToLoopStart();
        }
        return (boolean) DecorationOps.callsite().invoke(entity, vehicle);
    }

    @Decorate(method = "pushEntities", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;push(Lnet/minecraft/world/entity/Entity;)V", ordinal = 0))
    private void arclight$rideablePushCollision(Entity entity, Entity vehicle) throws Throwable {
        if (!this.minecart.isPassengerOfSameVehicle(entity)) {
            VehicleEntityCollisionEvent event = new VehicleEntityCollisionEvent((Vehicle) ((EntityBridge) this.minecart).bridge$getBukkitEntity(), ((EntityBridge) entity).bridge$getBukkitEntity());
            Bukkit.getPluginManager().callEvent(event);
            if (event.isCancelled()) {
                throw DecorationOps.jumpToLoopStart();
            }
        }
        DecorationOps.callsite().invoke(entity, vehicle);
    }

    @Decorate(method = "pushEntities", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;push(Lnet/minecraft/world/entity/Entity;)V", ordinal = 1))
    private void arclight$cartPushCollision(Entity entity, Entity vehicle) throws Throwable {
        VehicleEntityCollisionEvent event = new VehicleEntityCollisionEvent((Vehicle) ((EntityBridge) this.minecart).bridge$getBukkitEntity(), ((EntityBridge) entity).bridge$getBukkitEntity());
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            throw DecorationOps.jumpToLoopStart();
        }
        DecorationOps.callsite().invoke(entity, vehicle);
    }
}
