package io.izzel.arclight.common.mixin.core.world.entity.monster;

import io.izzel.arclight.common.bridge.core.world.entity.LivingEntityBridge;
import io.izzel.arclight.common.bridge.core.world.level.WorldBridge;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import net.minecraft.world.entity.ConversionParams;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import org.bukkit.entity.ZombieVillager;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.event.entity.EntityTransformEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;
import java.util.Set;
import java.util.function.Predicate;

@Mixin(net.minecraft.world.entity.monster.zombie.ZombieVillager.class)
public abstract class ZombieVillagerMixin extends ZombieMixin {

    @Inject(method = "startConverting", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/monster/zombie/ZombieVillager;removeEffect(Lnet/minecraft/core/Holder;)Z"))
    private void arclight$convert1(UUID conversionStarterIn, int conversionTimeIn, CallbackInfo ci) {
        this.persist = true;
        bridge$pushEffectCause(EntityPotionEffectEvent.Cause.CONVERSION);
    }

    @Inject(method = "startConverting", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/monster/zombie/ZombieVillager;addEffect(Lnet/minecraft/world/effect/MobEffectInstance;)Z"))
    private void arclight$convert2(UUID conversionStarterIn, int conversionTimeIn, CallbackInfo ci) {
        bridge$pushEffectCause(EntityPotionEffectEvent.Cause.CONVERSION);
    }

    @SuppressWarnings("unchecked")
    @Decorate(method = "finishConversion", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/monster/zombie/ZombieVillager;convertTo(Lnet/minecraft/world/entity/EntityType;Lnet/minecraft/world/entity/ConversionParams;Lnet/minecraft/world/entity/ConversionParams$AfterConversion;)Lnet/minecraft/world/entity/Mob;"))
    private <T extends Mob> T arclight$cure(net.minecraft.world.entity.monster.zombie.ZombieVillager zombieVillagerEntity, EntityType<T> entityType, ConversionParams conversionParams, ConversionParams.AfterConversion<T> afterConversion) throws Throwable {
        ((WorldBridge) this.level()).bridge$pushAddEntityReason(CreatureSpawnEvent.SpawnReason.CURED);
        this.bridge$pushTransformReason(EntityTransformEvent.TransformReason.CURED);
        ConversionParams.AfterConversion<T> callback = converted -> {
            ((LivingEntityBridge) converted).bridge$pushEffectCause(EntityPotionEffectEvent.Cause.CONVERSION);
            afterConversion.finalizeConversion(converted);
        };
        T converted = (T) DecorationOps.callsite().invoke(zombieVillagerEntity, entityType, conversionParams, callback);
        if (converted == null) {
            ((ZombieVillager) this.bridge$getBukkitEntity()).setConversionTime(-1);
        }
        return converted;
    }

    @SuppressWarnings("unchecked")
    @Decorate(method = "*", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/monster/zombie/ZombieVillager;dropPreservedEquipment(Lnet/minecraft/server/level/ServerLevel;Ljava/util/function/Predicate;)Ljava/util/Set;"))
    private Set<EquipmentSlot> arclight$forceDrop(net.minecraft.world.entity.monster.zombie.ZombieVillager zombieVillager, ServerLevel level, Predicate<ItemStack> predicate, ServerLevel capturedLevel, net.minecraft.world.entity.npc.villager.Villager villager) throws Throwable {
        boolean forceDrops = this.forceDrops;
        this.forceDrops = true;
        try {
            return (Set<EquipmentSlot>) DecorationOps.callsite().invoke(zombieVillager, level, predicate);
        } finally {
            this.forceDrops = forceDrops;
        }
    }
}
