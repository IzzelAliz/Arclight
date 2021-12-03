package io.izzel.arclight.common.mixin.core.world.entity.monster;

import io.izzel.arclight.common.bridge.core.entity.LivingEntityBridge;
import io.izzel.arclight.mixin.Eject;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import org.bukkit.entity.ZombieVillager;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.event.entity.EntityTransformEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(net.minecraft.world.entity.monster.ZombieVillager.class)
public abstract class ZombieVillagerMixin extends ZombieMixin {

    @Inject(method = "startConverting", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/monster/ZombieVillager;removeEffect(Lnet/minecraft/world/effect/MobEffect;)Z"))
    private void arclight$convert1(UUID conversionStarterIn, int conversionTimeIn, CallbackInfo ci) {
        this.persist = true;
        bridge$pushEffectCause(EntityPotionEffectEvent.Cause.CONVERSION);
    }

    @Inject(method = "startConverting", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/monster/ZombieVillager;addEffect(Lnet/minecraft/world/effect/MobEffectInstance;)Z"))
    private void arclight$convert2(UUID conversionStarterIn, int conversionTimeIn, CallbackInfo ci) {
        bridge$pushEffectCause(EntityPotionEffectEvent.Cause.CONVERSION);
    }

    @Eject(method = "finishConversion", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/monster/ZombieVillager;convertTo(Lnet/minecraft/world/entity/EntityType;Z)Lnet/minecraft/world/entity/Mob;"))
    private <T extends Mob> T arclight$cure(net.minecraft.world.entity.monster.ZombieVillager zombieVillagerEntity, EntityType<T> entityType, boolean flag, CallbackInfo ci) {
        T t = this.convertTo(entityType, flag, EntityTransformEvent.TransformReason.CURED, CreatureSpawnEvent.SpawnReason.CURED);
        if (t == null) {
            ((ZombieVillager) this.bridge$getBukkitEntity()).setConversionTime(-1);
            ci.cancel();
        } else {
            ((LivingEntityBridge) t).bridge$pushEffectCause(EntityPotionEffectEvent.Cause.CONVERSION);
        }
        return t;
    }

    @Inject(method = "finishConversion", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/monster/ZombieVillager;spawnAtLocation(Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/world/entity/item/ItemEntity;"))
    private void arclight$dropPre(ServerLevel world, CallbackInfo ci) {
        this.forceDrops = true;
    }

    @Inject(method = "finishConversion", at = @At(value = "INVOKE", shift = At.Shift.AFTER, target = "Lnet/minecraft/world/entity/monster/ZombieVillager;spawnAtLocation(Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/world/entity/item/ItemEntity;"))
    private void arclight$dropPost(ServerLevel world, CallbackInfo ci) {
        this.forceDrops = false;
    }
}
