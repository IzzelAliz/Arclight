package io.izzel.arclight.common.mixin.core.world.entity;

import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.phys.Vec3;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import io.izzel.arclight.common.bridge.core.world.entity.LivingEntityBridge;
import io.izzel.arclight.common.bridge.core.world.entity.MobBridge;
import io.izzel.arclight.common.bridge.core.world.entity.animal.PigBridge;
import io.izzel.arclight.common.bridge.core.world.level.WorldBridge;
import io.izzel.arclight.common.mod.server.ArclightServer;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.ConversionParams;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.DropChances;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.zombie.ZombifiedPiglin;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.entity.CraftLivingEntity;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityKnockbackEvent;
import org.bukkit.event.entity.EntityRemoveEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.entity.EntityTransformEvent;
import org.bukkit.event.entity.EntityUnleashEvent;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;

@Mixin(Mob.class)
public abstract class MobMixin extends LivingEntityMixin implements MobBridge {

    // @formatter:off
    @Shadow public boolean persistenceRequired;
    @Shadow public abstract boolean removeWhenFarAway(double distanceToClosestPlayer);
    @Shadow @Nullable public abstract LivingEntity getTarget();
    @Shadow private LivingEntity target;
    @Shadow public abstract boolean canHoldItem(ItemStack stack);
    @Shadow public abstract DropChances getDropChances();
    @Shadow public abstract void setDropChance(EquipmentSlot equipmentSlot, float chance);
    @Shadow public abstract boolean isPersistenceRequired();
    @Shadow public abstract boolean isNoAi();
    @Shadow protected abstract boolean canReplaceCurrentItem(ItemStack candidate, ItemStack existing, EquipmentSlot slot);
    @Shadow protected abstract void setItemSlotAndDropWhenKilled(EquipmentSlot p_233657_1_, ItemStack p_233657_2_);
    @Shadow @Nullable public abstract <T extends Mob> T convertTo(EntityType<T> entityType, ConversionParams conversionParams, EntitySpawnReason spawnReason, ConversionParams.AfterConversion<T> afterConversion);
    @Shadow @Nullable protected abstract SoundEvent getAmbientSound();
    @Shadow public abstract void setTarget(@org.jetbrains.annotations.Nullable LivingEntity livingEntity);
    // @formatter:on

    public boolean aware;

    @Override
    public void bridge$setAware(boolean aware) {
        this.aware = aware;
    }

    @Inject(method = "setCanPickUpLoot", at = @At("HEAD"))
    public void arclight$setPickupLoot(boolean canPickup, CallbackInfo ci) {
        super.bukkitPickUpLoot = canPickup;
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public boolean canPickUpLoot() {
        return super.bukkitPickUpLoot;
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void arclight$init(EntityType<? extends Mob> type, net.minecraft.world.level.Level worldIn, CallbackInfo ci) {
        this.aware = true;
    }

    public SoundEvent getAmbientSound0() {
        return getAmbientSound();
    }

    protected transient boolean arclight$targetSuccess = false;
    private transient EntityTargetEvent.TargetReason arclight$reason;
    private transient boolean arclight$fireEvent;
    private transient EntityTargetEvent.TargetReason arclight$activeTargetReason;
    private transient boolean arclight$activeTargetEvent;
    private transient boolean arclight$activeTargetSuccess;

    @WrapMethod(method = "setTarget(Lnet/minecraft/world/entity/LivingEntity;)V")
    private void arclight$targetScope(LivingEntity livingEntity, Operation<Void> original) {
        EntityTargetEvent.TargetReason parentReason = arclight$activeTargetReason;
        boolean parentEvent = arclight$activeTargetEvent;
        boolean parentSuccess = arclight$activeTargetSuccess;
        arclight$activeTargetReason = arclight$reason;
        arclight$activeTargetEvent = arclight$fireEvent;
        arclight$activeTargetSuccess = false;
        arclight$reason = null;
        arclight$fireEvent = false;
        try {
            original.call(livingEntity);
        } finally {
            arclight$targetSuccess = arclight$activeTargetSuccess;
            arclight$activeTargetReason = parentReason;
            arclight$activeTargetEvent = parentEvent;
            arclight$activeTargetSuccess = parentSuccess;
        }
    }

    @Decorate(method = "setTarget", at = @At(value = "FIELD", opcode = Opcodes.PUTFIELD, target = "Lnet/minecraft/world/entity/Mob;target:Lnet/minecraft/world/entity/LivingEntity;"))
    private void arclight$setTargetEvent(Mob instance, LivingEntity livingEntity) throws Throwable {
        boolean fireEvent = arclight$activeTargetEvent;
        arclight$activeTargetEvent = false;
        EntityTargetEvent.TargetReason reason = arclight$activeTargetReason == null ? EntityTargetEvent.TargetReason.UNKNOWN : arclight$activeTargetReason;
        arclight$activeTargetReason = null;
        // Compare the stored reference: getTarget() may hide a stale target as null.
        if (this.target == livingEntity) {
            arclight$activeTargetSuccess = false;
            return;
        }
        if (fireEvent) {
            if (reason == EntityTargetEvent.TargetReason.UNKNOWN && this.getTarget() != null && livingEntity == null) {
                reason = (this.getTarget().isAlive() ? EntityTargetEvent.TargetReason.FORGOT_TARGET : EntityTargetEvent.TargetReason.TARGET_DIED);
            }
            if (reason == EntityTargetEvent.TargetReason.UNKNOWN) {
                ArclightServer.LOGGER.debug("Unknown target reason setting {} target to {}", this, livingEntity);
            }
            CraftLivingEntity ctarget = null;
            if (livingEntity != null) {
                ctarget = ((LivingEntityBridge) livingEntity).bridge$getBukkitEntity();
            }
            final EntityTargetLivingEntityEvent event = new EntityTargetLivingEntityEvent(this.getBukkitEntity(), ctarget, reason);
            Bukkit.getPluginManager().callEvent(event);
            if (event.isCancelled()) {
                arclight$activeTargetSuccess = false;
                DecorationOps.cancel().invoke();
                return;
            }
            if (event.getTarget() != null) {
                livingEntity = ((CraftLivingEntity) event.getTarget()).getHandle();
            } else {
                livingEntity = null;
            }
        }
        DecorationOps.callsite().invoke(instance, livingEntity);
        arclight$activeTargetSuccess = true;
    }

    public boolean setTarget(LivingEntity livingEntity, EntityTargetEvent.TargetReason reason, boolean fireEvent) {
        bridge$pushGoalTargetReason(reason, fireEvent);
        setTarget(livingEntity);
        return arclight$targetSuccess;
    }

    @Override
    public boolean bridge$lastGoalTargetResult() {
        return arclight$targetSuccess;
    }

    @Override
    public boolean bridge$setGoalTarget(LivingEntity livingEntity, EntityTargetEvent.TargetReason reason, boolean fireEvent) {
        return setTarget(livingEntity, reason, fireEvent);
    }

    @Override
    public void bridge$pushGoalTargetReason(EntityTargetEvent.TargetReason reason, boolean fireEvent) {
        if (fireEvent) {
            this.arclight$reason = reason;
        } else {
            this.arclight$reason = null;
        }
        arclight$fireEvent = fireEvent;
    }

    @Inject(method = "addAdditionalSaveData", at = @At("HEAD"))
    private void arclight$setAware(ValueOutput output, CallbackInfo ci) {
        output.putBoolean("Bukkit.Aware", this.aware);
    }

    @Inject(method = "readAdditionalSaveData", at = @At("HEAD"))
    private void arclight$readAware(ValueInput input, CallbackInfo ci) {
        this.aware = input.getBooleanOr("Bukkit.Aware", true);
    }

    @Redirect(method = "readAdditionalSaveData", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Mob;setCanPickUpLoot(Z)V"))
    public void arclight$setIfTrue(Mob mobEntity, boolean canPickup) {
        if (canPickup) mobEntity.setCanPickUpLoot(true);
    }

    @Inject(method = "serverAiStep", cancellable = true, at = @At("HEAD"))
    private void arclight$unaware(CallbackInfo ci) {
        if (!this.aware) {
            ++this.noActionTime;
            ci.cancel();
        }
    }

    @Inject(method = "pickUpItem", at = @At("HEAD"))
    private void arclight$captureItemEntity(ServerLevel level, ItemEntity itemEntity, CallbackInfo ci) {
        arclight$item = itemEntity;
    }

    @Inject(method = "pickUpItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/item/ItemEntity;discard()V"))
    private void arclight$pickupCause(ServerLevel level, ItemEntity itemEntity, CallbackInfo ci) {
        itemEntity.bridge().bridge$pushEntityRemoveCause(EntityRemoveEvent.Cause.PICKUP);
    }

    @Override
    public void bridge$captureItemDrop(ItemEntity itemEntity) {
        this.arclight$item = itemEntity;
    }

    private transient ItemEntity arclight$item;

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public ItemStack equipItemIfPossible(ServerLevel level, ItemStack stack) {
        ItemEntity itemEntity = arclight$item;
        arclight$item = null;
        EquipmentSlot equipmentslottype = getEquipmentSlotForItem(stack);
        if (!((LivingEntity) (Object) this).isEquippableInSlot(stack, equipmentslottype)) {
            return ItemStack.EMPTY;
        }
        ItemStack itemstack = this.getItemBySlot(equipmentslottype);
        boolean flag = this.canReplaceCurrentItem(stack, itemstack, equipmentslottype);

        if (equipmentslottype.isArmor() && !flag) {
            equipmentslottype = EquipmentSlot.MAINHAND;
            itemstack = this.getItemBySlot(equipmentslottype);
            flag = itemstack.isEmpty();
        }

        boolean canPickup = flag && this.canHoldItem(stack);
        if (itemEntity != null) {
            canPickup = !CraftEventFactory.callEntityPickupItemEvent((Mob) (Object) this, itemEntity, 0, !canPickup).isCancelled();
        }
        if (canPickup) {
            double d0 = this.getDropChances().byEquipment(equipmentslottype);
            if (!itemstack.isEmpty() && (double) Math.max(this.random.nextFloat() - 0.1F, 0.0F) < d0) {
                forceDrops = true;
                this.spawnAtLocation(level, itemstack);
                forceDrops = false;
            }

            var itemstack2 = equipmentslottype.limit(stack);
            this.setItemSlotAndDropWhenKilled(equipmentslottype, itemstack2);
            return itemstack2;
        } else {
            return ItemStack.EMPTY;
        }
    }

    @Inject(method = "startRiding(Lnet/minecraft/world/entity/Entity;ZZ)Z", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Mob;dropLeash()V"))
    private void arclight$unleashRide(Entity entityIn, boolean force, boolean forcePassenger, CallbackInfoReturnable<Boolean> cir) {
        Bukkit.getPluginManager().callEvent(new EntityUnleashEvent(this.getBukkitEntity(), EntityUnleashEvent.UnleashReason.UNKNOWN));
    }

    private transient boolean arclight$conversionEquipment;
    private transient boolean arclight$conversionEquipmentDeferred;

    @WrapMethod(method = "convertTo(Lnet/minecraft/world/entity/EntityType;Lnet/minecraft/world/entity/ConversionParams;Lnet/minecraft/world/entity/EntitySpawnReason;Lnet/minecraft/world/entity/ConversionParams$AfterConversion;)Lnet/minecraft/world/entity/Mob;")
    private <T extends Mob> T arclight$conversionEquipment(EntityType<T> entityType, ConversionParams params, EntitySpawnReason spawnReason,
            ConversionParams.AfterConversion<T> afterConversion, Operation<T> original) {
        this.arclight$conversionEquipment = params.type() == net.minecraft.world.entity.ConversionType.SINGLE && params.keepEquipment();
        this.arclight$conversionEquipmentDeferred = false;
        try {
            return original.call(entityType, params, spawnReason, afterConversion);
        } finally {
            this.arclight$conversionEquipment = false;
            this.arclight$conversionEquipmentDeferred = false;
            this.arclight$transform = null;
            ((WorldBridge) this.level()).bridge$pushAddEntityReason(null);
        }
    }

    @Override
    public boolean bridge$deferConversionEquipment() {
        if (!this.arclight$conversionEquipment || this.bridge$neoforge$preventConversionSpawns()) return false;
        this.arclight$conversionEquipmentDeferred = true;
        return true;
    }

    @Override
    public void bridge$clearDeferredConversionEquipment() {
        if (this.arclight$conversionEquipmentDeferred) this.bridge$clearConversionEquipment();
    }

    @Override
    public void bridge$clearConversionEquipment() {
        if (!this.arclight$conversionEquipment) return;
        this.arclight$conversionEquipmentDeferred = false;
        for (net.minecraft.world.entity.EquipmentSlot slot : net.minecraft.world.entity.EquipmentSlot.VALUES) {
            ItemStack stack = ((Mob) (Object) this).getItemBySlot(slot);
            if (!stack.isEmpty()) stack.setCount(0);
        }
    }

    @WrapOperation(method = "convertTo(Lnet/minecraft/world/entity/EntityType;Lnet/minecraft/world/entity/ConversionParams;Lnet/minecraft/world/entity/EntitySpawnReason;Lnet/minecraft/world/entity/ConversionParams$AfterConversion;)Lnet/minecraft/world/entity/Mob;", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"))
    private boolean arclight$copySpawn(ServerLevel world, Entity entityIn, Operation<Boolean> original,
            EntityType<?> type, ConversionParams params, EntitySpawnReason spawnReason,
            ConversionParams.AfterConversion<?> after,
            @com.llamalad7.mixinextras.sugar.Cancellable CallbackInfoReturnable<Mob> cir) {
        if (params.type() == net.minecraft.world.entity.ConversionType.SPLIT_ON_DEATH
            && after instanceof io.izzel.arclight.common.mod.server.entity.SlimeSplitScope.Conversion<?> split
            && split.defer((Mob) (Object) this, entityIn, () -> original.call(world, entityIn))) {
            return true;
        }
        EntityTransformEvent.TransformReason transformReason = arclight$transform == null
            ? EntityTransformEvent.TransformReason.UNKNOWN : arclight$transform;
        this.arclight$transform = null;
        boolean pigZapDispatched = false;
        if ((Object) this instanceof Pig pig && entityIn instanceof ZombifiedPiglin piglin) {
            PigBridge pigBridge = (PigBridge) pig;
            var lightningBolt = pigBridge.bridge$getPigZapLightning();
            if (lightningBolt != null) {
                pigBridge.bridge$clearPigZapLightning();
                pigZapDispatched = true;
                if (CraftEventFactory.callPigZapEvent(pig, lightningBolt, piglin).isCancelled()) {
                    pigBridge.bridge$cancelPigZap();
                    cir.setReturnValue(null);
                    return false;
                }
            }
        }
        // PigZapEvent owns the lightning-pig conversion; do not dispatch a second transform event.
        if (!pigZapDispatched && CraftEventFactory.callEntityTransformEvent((Mob) (Object) this, (LivingEntity) entityIn, transformReason).isCancelled()) {
            cir.setReturnValue(null);
            return false;
        } else {
            this.bridge$clearConversionEquipment();
            return original.call(world, entityIn);
        }
    }

    @Inject(method = "convertTo(Lnet/minecraft/world/entity/EntityType;Lnet/minecraft/world/entity/ConversionParams;Lnet/minecraft/world/entity/EntitySpawnReason;Lnet/minecraft/world/entity/ConversionParams$AfterConversion;)Lnet/minecraft/world/entity/Mob;", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Mob;discard()V"))
    private <T extends Mob> void arclight$transformCause(EntityType<T> entityType, ConversionParams conversionParams, EntitySpawnReason spawnReason, ConversionParams.AfterConversion<T> afterConversion, CallbackInfoReturnable<T> cir) {
        this.bridge$pushEntityRemoveCause(EntityRemoveEvent.Cause.TRANSFORMATION);
    }

    public <T extends Mob> T convertTo(EntityType<T> entityType, boolean flag, EntityTransformEvent.TransformReason transformReason, CreatureSpawnEvent.SpawnReason spawnReason) {
        ((WorldBridge) this.level()).bridge$pushAddEntityReason(spawnReason);
        bridge$pushTransformReason(transformReason);
        return this.convertTo(entityType, ConversionParams.single((Mob) (Object) this, flag, flag), EntitySpawnReason.CONVERSION, mob -> {});
    }

    private transient EntityTransformEvent.TransformReason arclight$transform;

    @Override
    public EntityTransformEvent.TransformReason bridge$getTransformReason() {
        return this.arclight$transform;
    }


    @Override
    public void bridge$pushTransformReason(EntityTransformEvent.TransformReason transformReason) {
        this.arclight$transform = transformReason;
    }

    @Inject(method = "doHurtTarget(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/Entity;)Z", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Mob;causeExtraKnockback(Lnet/minecraft/world/entity/Entity;FLnet/minecraft/world/phys/Vec3;)V"))
    private void arclight$attackKnockback(ServerLevel level, Entity entity, CallbackInfoReturnable<Boolean> cir) {
        ((LivingEntityBridge) entity).bridge$pushKnockbackCause((Entity) (Object) this, EntityKnockbackEvent.KnockbackCause.ENTITY_ATTACK);
    }

    @Inject(method = "checkDespawn", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Mob;discard()V"))
    private void arclight$naturalDespawn(CallbackInfo ci) {
        this.bridge$pushEntityRemoveCause(EntityRemoveEvent.Cause.DESPAWN);
    }

    @Override
    public boolean bridge$isPersistenceRequired() {
        return this.persistenceRequired;
    }

    public void setPersistenceRequired(boolean value) {
        this.persistenceRequired = value;
    }

    @Override
    public void bridge$setPersistenceRequired(boolean value) {
        this.setPersistenceRequired(value);
    }

    @Override
    public boolean bridge$common$animalTameEvent(Player player) {
        return !CraftEventFactory.callEntityTameEvent((Mob) (Object) this, player).isCancelled();
    }
}
