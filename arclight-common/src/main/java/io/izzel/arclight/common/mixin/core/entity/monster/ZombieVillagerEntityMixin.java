package io.izzel.arclight.common.mixin.core.entity.monster;

import io.izzel.arclight.common.bridge.entity.LivingEntityBridge;
import io.izzel.arclight.mixin.Eject;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.monster.ZombieVillagerEntity;
import org.bukkit.entity.ZombieVillager;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.event.entity.EntityTransformEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(ZombieVillagerEntity.class)
public abstract class ZombieVillagerEntityMixin extends ZombieEntityMixin {

    @Inject(method = "startConverting", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/monster/ZombieVillagerEntity;removePotionEffect(Lnet/minecraft/potion/Effect;)Z"))
    private void arclight$convert1(UUID conversionStarterIn, int conversionTimeIn, CallbackInfo ci) {
        this.persist = true;
        bridge$pushEffectCause(EntityPotionEffectEvent.Cause.CONVERSION);
    }

    @Inject(method = "startConverting", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/monster/ZombieVillagerEntity;addPotionEffect(Lnet/minecraft/potion/EffectInstance;)Z"))
    private void arclight$convert2(UUID conversionStarterIn, int conversionTimeIn, CallbackInfo ci) {
        bridge$pushEffectCause(EntityPotionEffectEvent.Cause.CONVERSION);
    }

    @Eject(method = "cureZombie", at = @At(value = "INVOKE", remap = false, target = "Lnet/minecraft/entity/monster/ZombieVillagerEntity;func_233656_b_(Lnet/minecraft/entity/EntityType;Z)Lnet/minecraft/entity/MobEntity;"))
    private <T extends MobEntity> T arclight$cure(ZombieVillagerEntity zombieVillagerEntity, EntityType<T> entityType, boolean flag, CallbackInfo ci) {
        T t = this.a(entityType, flag, EntityTransformEvent.TransformReason.CURED, CreatureSpawnEvent.SpawnReason.CURED);
        if (t == null) {
            ((ZombieVillager) this.bridge$getBukkitEntity()).setConversionTime(-1);
            ci.cancel();
        } else {
            ((LivingEntityBridge) t).bridge$pushEffectCause(EntityPotionEffectEvent.Cause.CONVERSION);
        }
        return t;
    }
}
