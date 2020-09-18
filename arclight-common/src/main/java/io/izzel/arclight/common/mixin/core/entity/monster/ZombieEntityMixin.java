package io.izzel.arclight.common.mixin.core.entity.monster;

import io.izzel.arclight.common.bridge.entity.EntityBridge;
import io.izzel.arclight.common.bridge.entity.MobEntityBridge;
import io.izzel.arclight.common.bridge.world.WorldBridge;
import io.izzel.arclight.common.mixin.core.entity.CreatureEntityMixin;
import io.izzel.arclight.mixin.Eject;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ILivingEntityData;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.merchant.villager.VillagerEntity;
import net.minecraft.entity.monster.ZombieEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.DamageSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.IServerWorld;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.event.entity.living.ZombieEvent;
import org.bukkit.Bukkit;
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

    @Inject(method = "func_234341_c_", at = @At("HEAD"))
    private void arclight$transformReason(EntityType<? extends ZombieEntity> entityType, CallbackInfo ci) {
        this.bridge$pushTransformReason(EntityTransformEvent.TransformReason.DROWNED);
        ((WorldBridge) this.world).bridge$pushAddEntityReason(CreatureSpawnEvent.SpawnReason.DROWNED);
    }

    @Inject(method = "func_234341_c_", locals = LocalCapture.CAPTURE_FAILHARD, at = @At("RETURN"))
    private void arclight$stopConversion(EntityType<? extends ZombieEntity> entityType, CallbackInfo ci, ZombieEntity zombieEntity) {
        if (zombieEntity == null) {
            ((Zombie) this.bridge$getBukkitEntity()).setConversionTime(-1);
        }
    }

    @Inject(method = "attackEntityFrom", locals = LocalCapture.CAPTURE_FAILHARD, at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/monster/ZombieEntity;onInitialSpawn(Lnet/minecraft/world/IServerWorld;Lnet/minecraft/world/DifficultyInstance;Lnet/minecraft/entity/SpawnReason;Lnet/minecraft/entity/ILivingEntityData;Lnet/minecraft/nbt/CompoundNBT;)Lnet/minecraft/entity/ILivingEntityData;"))
    private void arclight$spawnWithReason(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir, ServerWorld world, LivingEntity livingEntity, int i, int j, int k, ZombieEvent.SummonAidEvent event, ZombieEntity zombieEntity) {
        ((WorldBridge) world).bridge$pushAddEntityReason(CreatureSpawnEvent.SpawnReason.REINFORCEMENTS);
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

    @Eject(method = "func_241847_a", at = @At(value = "INVOKE", remap = false, target = "Lnet/minecraft/entity/merchant/villager/VillagerEntity;func_233656_b_(Lnet/minecraft/entity/EntityType;Z)Lnet/minecraft/entity/MobEntity;"))
    private <T extends MobEntity> T arclight$transform(VillagerEntity villagerEntity, EntityType<T> entityType, boolean flag, CallbackInfo ci) {
        T t = this.a(entityType, flag, EntityTransformEvent.TransformReason.INFECTION, CreatureSpawnEvent.SpawnReason.INFECTION);
        if (t == null) {
            ci.cancel();
        }
        return t;
    }

    @Inject(method = "onInitialSpawn", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/IServerWorld;addEntity(Lnet/minecraft/entity/Entity;)Z"))
    private void arclight$mount(IServerWorld worldIn, DifficultyInstance difficultyIn, SpawnReason reason, ILivingEntityData spawnDataIn, CompoundNBT dataTag, CallbackInfoReturnable<ILivingEntityData> cir) {
        ((WorldBridge) worldIn.getWorld()).bridge$pushAddEntityReason(CreatureSpawnEvent.SpawnReason.MOUNT);
    }
}
