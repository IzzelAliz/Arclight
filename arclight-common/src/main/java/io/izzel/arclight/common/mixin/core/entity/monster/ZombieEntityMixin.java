package io.izzel.arclight.common.mixin.core.entity.monster;

import io.izzel.arclight.common.bridge.entity.EntityBridge;
import io.izzel.arclight.common.bridge.entity.MobEntityBridge;
import io.izzel.arclight.common.bridge.world.WorldBridge;
import io.izzel.arclight.common.mixin.core.entity.CreatureEntityMixin;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ILivingEntityData;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.merchant.villager.VillagerEntity;
import net.minecraft.entity.monster.ZombieEntity;
import net.minecraft.entity.monster.ZombieVillagerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.DamageSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.IWorld;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.bukkit.entity.Zombie;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityCombustByEntityEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.EntityTransformEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(ZombieEntity.class)
public abstract class ZombieEntityMixin extends CreatureEntityMixin {

    @Inject(method = "func_213698_b", cancellable = true, locals = LocalCapture.CAPTURE_FAILHARD,
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;addEntity(Lnet/minecraft/entity/Entity;)Z"))
    private void arclight$drown(EntityType<? extends ZombieEntity> p_213698_1_, CallbackInfo ci, ZombieEntity zombieEntity) {
        if (CraftEventFactory.callEntityTransformEvent((ZombieEntity) (Object) this, zombieEntity, EntityTransformEvent.TransformReason.DROWNED).isCancelled()) {
            ((Zombie) getBukkitEntity()).setConversionTime(-1);
            ci.cancel();
        } else {
            ((WorldBridge) this.world).bridge$pushAddEntityReason(CreatureSpawnEvent.SpawnReason.DROWNED);
        }
    }

    @Inject(method = "attackEntityFrom", locals = LocalCapture.CAPTURE_FAILHARD, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;addEntity(Lnet/minecraft/entity/Entity;)Z"))
    private void arclight$spawnWithReason(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir, LivingEntity livingEntity, int i, int j, int k, net.minecraftforge.event.entity.living.ZombieEvent.SummonAidEvent event, ZombieEntity zombieEntity) {
        ((WorldBridge) this.world).bridge$pushAddEntityReason(CreatureSpawnEvent.SpawnReason.REINFORCEMENTS);
        if (livingEntity != null) {
            ((MobEntityBridge) zombieEntity).bridge$pushGoalTargetReason(EntityTargetEvent.TargetReason.REINFORCEMENT_TARGET, true);
        }
    }

    @Redirect(method = "attackEntityAsMob", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;setFire(I)V"))
    private void arclight$entityCombust(Entity entity, int seconds) {
        EntityCombustByEntityEvent event = new EntityCombustByEntityEvent(this.getBukkitEntity(), ((EntityBridge) entity).bridge$getBukkitEntity(), seconds);
        Bukkit.getPluginManager().callEvent(event);
        if (!event.isCancelled()) {
            ((EntityBridge) entity).bridge$setOnFire(event.getDuration(), false);
        }
    }

    @Redirect(method = "onKillEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/merchant/villager/VillagerEntity;remove()V"))
    private void arclight$transformPre(VillagerEntity villagerEntity) {
    }

    @Inject(method = "onKillEntity", cancellable = true, locals = LocalCapture.CAPTURE_FAILHARD, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;addEntity(Lnet/minecraft/entity/Entity;)Z"))
    private void arclight$transformInfection(LivingEntity entityLivingIn, CallbackInfo ci, VillagerEntity villagerEntity, ZombieVillagerEntity zombieVillagerEntity) {
        if (CraftEventFactory.callEntityTransformEvent(villagerEntity, zombieVillagerEntity, EntityTransformEvent.TransformReason.INFECTION).isCancelled()) {
            ci.cancel();
        } else {
            villagerEntity.remove();
            ((WorldBridge) this.world).bridge$pushAddEntityReason(CreatureSpawnEvent.SpawnReason.INFECTION);
        }
    }

    @Inject(method = "onInitialSpawn", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/IWorld;addEntity(Lnet/minecraft/entity/Entity;)Z"))
    private void arclight$mount(IWorld worldIn, DifficultyInstance difficultyIn, SpawnReason reason, ILivingEntityData spawnDataIn, CompoundNBT dataTag, CallbackInfoReturnable<ILivingEntityData> cir) {
        ((WorldBridge) worldIn).bridge$pushAddEntityReason(CreatureSpawnEvent.SpawnReason.MOUNT);
    }
}
