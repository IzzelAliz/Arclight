package io.izzel.arclight.common.mixin.core.entity.monster;

import io.izzel.arclight.common.bridge.world.WorldBridge;
import net.minecraft.entity.ILivingEntityData;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.monster.SpiderEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.IWorld;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import io.izzel.arclight.common.mixin.core.entity.CreatureEntityMixin;

@Mixin(SpiderEntity.class)
public abstract class SpiderEntityMixin extends CreatureEntityMixin {

    @Inject(method = "onInitialSpawn", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/IWorld;addEntity(Lnet/minecraft/entity/Entity;)Z"))
    private void arclight$spawnReason(IWorld worldIn, DifficultyInstance difficultyIn, SpawnReason reason, ILivingEntityData spawnDataIn, CompoundNBT dataTag, CallbackInfoReturnable<ILivingEntityData> cir) {
        ((WorldBridge) worldIn).bridge$pushAddEntityReason(CreatureSpawnEvent.SpawnReason.JOCKEY);
    }

    @Inject(method = "onInitialSpawn", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/monster/SpiderEntity;addPotionEffect(Lnet/minecraft/potion/EffectInstance;)Z"))
    private void arclight$potionReason(IWorld worldIn, DifficultyInstance difficultyIn, SpawnReason reason, ILivingEntityData spawnDataIn, CompoundNBT dataTag, CallbackInfoReturnable<ILivingEntityData> cir) {
        bridge$pushEffectCause(EntityPotionEffectEvent.Cause.SPIDER_SPAWN);
    }
}
