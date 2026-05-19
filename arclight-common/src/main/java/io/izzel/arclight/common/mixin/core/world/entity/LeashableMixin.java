package io.izzel.arclight.common.mixin.core.world.entity;

import io.izzel.arclight.common.bridge.core.entity.EntityBridge;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Leashable;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.storage.ValueOutput;
import org.bukkit.Bukkit;
import org.bukkit.event.entity.EntityUnleashEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Leashable.class)
public interface LeashableMixin {

    @Decorate(method = "writeLeashData", inject = true, at = @At("HEAD"))
    private void arclight$skipRemoved(ValueOutput output, Leashable.LeashData leashData) throws Throwable {
        if (leashData != null && leashData.leashHolder != null && ((EntityBridge) leashData.leashHolder).bridge$pluginRemoved()) {
            DecorationOps.cancel().invoke();
            return;
        }
        DecorationOps.blackhole().invoke();
    }

    @Decorate(method = "restoreLeashFromSave", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;spawnAtLocation(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/level/ItemLike;)Lnet/minecraft/world/entity/item/ItemEntity;"))
    private static ItemEntity arclight$forceDrop(Entity instance, ServerLevel serverLevel, ItemLike itemLike) throws Throwable {
        instance.bridge().bridge$setForceDrops(true);
        var itemEntity = (ItemEntity) DecorationOps.callsite().invoke(instance, serverLevel, itemLike);
        instance.bridge().bridge$setForceDrops(false);
        return itemEntity;
    }

    @Decorate(method = "dropLeash(Lnet/minecraft/world/entity/Entity;ZZ)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;spawnAtLocation(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/level/ItemLike;)Lnet/minecraft/world/entity/item/ItemEntity;"))
    private static ItemEntity arclight$forceDrop2(Entity instance, ServerLevel serverLevel, ItemLike itemLike) throws Throwable {
        instance.bridge().bridge$setForceDrops(true);
        var itemEntity = (ItemEntity) DecorationOps.callsite().invoke(instance, serverLevel, itemLike);
        instance.bridge().bridge$setForceDrops(false);
        return itemEntity;
    }

    @Decorate(method = "tickLeash(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/Entity;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Leashable;dropLeash()V"))
    private static void arclight$unleashDrop(Leashable leashable) throws Throwable {
        Entity entity = (Entity) leashable;
        Bukkit.getPluginManager().callEvent(new EntityUnleashEvent(entity.bridge$getBukkitEntity(), (!entity.isAlive())
            ? EntityUnleashEvent.UnleashReason.PLAYER_UNLEASH : EntityUnleashEvent.UnleashReason.HOLDER_GONE));
        if (entity.bridge().bridge$pluginRemoved()) {
            leashable.removeLeash();
        } else {
            DecorationOps.callsite().invoke(leashable);
        }
    }

    @Decorate(method = "tickLeash(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/Entity;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Leashable;removeLeash()V"))
    private static void arclight$unleashRemove(Leashable leashable) throws Throwable {
        Entity entity = (Entity) leashable;
        Bukkit.getPluginManager().callEvent(new EntityUnleashEvent(entity.bridge$getBukkitEntity(), (!entity.isAlive())
            ? EntityUnleashEvent.UnleashReason.PLAYER_UNLEASH : EntityUnleashEvent.UnleashReason.HOLDER_GONE));
        DecorationOps.callsite().invoke(leashable);
    }

    @Decorate(method = "leashTooFarBehaviour", inject = true, at = @At("HEAD"))
    private void arclight$distanceLeash() {
        if (this instanceof Entity entity) {
            Bukkit.getPluginManager().callEvent(new EntityUnleashEvent(entity.bridge$getBukkitEntity(), EntityUnleashEvent.UnleashReason.DISTANCE));
        }
    }
}
