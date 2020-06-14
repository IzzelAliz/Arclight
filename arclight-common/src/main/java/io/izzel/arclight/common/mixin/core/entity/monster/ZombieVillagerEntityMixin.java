package io.izzel.arclight.common.mixin.core.entity.monster;

import io.izzel.arclight.common.bridge.entity.LivingEntityBridge;
import io.izzel.arclight.common.bridge.world.WorldBridge;
import net.minecraft.entity.merchant.villager.VillagerEntity;
import net.minecraft.entity.monster.ZombieVillagerEntity;
import net.minecraft.world.server.ServerWorld;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.bukkit.entity.ZombieVillager;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.event.entity.EntityTransformEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

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

    @Redirect(method = "cureZombie", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/monster/ZombieVillagerEntity;remove()V"))
    private void arclight$transformPre(ZombieVillagerEntity zombieVillagerEntity) {
    }

    @Inject(method = "cureZombie", cancellable = true, locals = LocalCapture.CAPTURE_FAILHARD, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/server/ServerWorld;addEntity(Lnet/minecraft/entity/Entity;)Z"))
    private void arclight$transform(ServerWorld world, CallbackInfo ci, VillagerEntity villagerEntity) {
        if (CraftEventFactory.callEntityTransformEvent((ZombieVillagerEntity)(Object)this, villagerEntity, EntityTransformEvent.TransformReason.CURED).isCancelled()) {
            ((ZombieVillager) getBukkitEntity()).setConversionTime(-1);
            ci.cancel();
        } else {
            this.remove();
            ((WorldBridge) world).bridge$pushAddEntityReason(CreatureSpawnEvent.SpawnReason.CURED);
            ((LivingEntityBridge) villagerEntity).bridge$pushEffectCause(EntityPotionEffectEvent.Cause.CONVERSION);
        }
    }
}
