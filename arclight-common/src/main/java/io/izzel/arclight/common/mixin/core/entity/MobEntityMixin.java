package io.izzel.arclight.common.mixin.core.entity;

import io.izzel.arclight.common.bridge.entity.EntityBridge;
import io.izzel.arclight.common.bridge.entity.LivingEntityBridge;
import io.izzel.arclight.common.bridge.entity.MobEntityBridge;
import io.izzel.arclight.common.bridge.world.WorldBridge;
import io.izzel.arclight.common.mod.ArclightMod;
import io.izzel.arclight.mixin.Eject;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.play.server.SMountEntityPacket;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeHooks;
import org.apache.logging.log4j.Level;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v.entity.CraftLivingEntity;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityCombustByEntityEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.entity.EntityTransformEvent;
import org.bukkit.event.entity.EntityUnleashEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Implements;
import org.spongepowered.asm.mixin.Interface;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;

@Mixin(MobEntity.class)
@Implements(@Interface(iface = MobEntityBridge.Hack.class, prefix = "hack$"))
public abstract class MobEntityMixin extends LivingEntityMixin implements MobEntityBridge {

    // @formatter:off
    @Shadow public boolean persistenceRequired;
    @Shadow public abstract boolean canDespawn(double distanceToClosestPlayer);
    @Shadow @Nullable public abstract LivingEntity getAttackTarget();
    @Shadow private LivingEntity attackTarget;
    @Shadow protected abstract ResourceLocation shadow$getLootTable();
    @Shadow public static EquipmentSlotType getSlotForItemStack(ItemStack stack) { return null; }
    @Shadow public abstract ItemStack getItemStackFromSlot(EquipmentSlotType slotIn);
    @Shadow public abstract boolean canEquipItem(ItemStack stack);
    @Shadow protected abstract float getDropChance(EquipmentSlotType slotIn);
    @Shadow public abstract void setItemStackToSlot(EquipmentSlotType slotIn, ItemStack stack);
    @Shadow @Final public float[] inventoryHandsDropChances;
    @Shadow @Final public float[] inventoryArmorDropChances;
    @Shadow @Nullable public abstract Entity getLeashHolder();
    @Shadow public abstract boolean isNoDespawnRequired();
    @Shadow protected void updateAITasks() { }
    @Shadow public abstract boolean isAIDisabled();
    @Shadow protected abstract boolean shouldExchangeEquipment(ItemStack candidate, ItemStack existing);
    @Shadow protected abstract void func_233657_b_(EquipmentSlotType p_233657_1_, ItemStack p_233657_2_);
    @Shadow @Nullable public abstract <T extends MobEntity> T func_233656_b_(EntityType<T> p_233656_1_, boolean p_233656_2_);
    // @formatter:on

    public boolean aware;

    @Override
    public void bridge$setAware(boolean aware) {
        this.aware = aware;
    }

    @Inject(method = "setCanPickUpLoot", at = @At("HEAD"))
    public void arclight$setPickupLoot(boolean canPickup, CallbackInfo ci) {
        super.canPickUpLoot = canPickup;
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public boolean canPickUpLoot() {
        return super.canPickUpLoot;
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void arclight$init(EntityType<? extends MobEntity> type, World worldIn, CallbackInfo ci) {
        this.persistenceRequired = !this.canDespawn(0.0);
        this.aware = true;
    }

    protected transient boolean arclight$targetSuccess = false;
    private transient EntityTargetEvent.TargetReason arclight$reason;
    private transient boolean arclight$fireEvent;

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public void setAttackTarget(@Nullable LivingEntity livingEntity) {
        boolean fireEvent = arclight$fireEvent;
        arclight$fireEvent = false;
        EntityTargetEvent.TargetReason reason = arclight$reason == null ? EntityTargetEvent.TargetReason.UNKNOWN : arclight$reason;
        arclight$reason = null;
        if (getAttackTarget() == livingEntity) {
            arclight$targetSuccess = false;
            return;
        }
        if (fireEvent) {
            if (reason == EntityTargetEvent.TargetReason.UNKNOWN && this.getAttackTarget() != null && livingEntity == null) {
                reason = (this.getAttackTarget().isAlive() ? EntityTargetEvent.TargetReason.FORGOT_TARGET : EntityTargetEvent.TargetReason.TARGET_DIED);
            }
            if (reason == EntityTargetEvent.TargetReason.UNKNOWN) {
                ArclightMod.LOGGER.log(Level.WARN, "Unknown target reason setting {} target to {}", this, livingEntity);
            }
            CraftLivingEntity ctarget = null;
            if (livingEntity != null) {
                ctarget = ((LivingEntityBridge) livingEntity).bridge$getBukkitEntity();
            }
            final EntityTargetLivingEntityEvent event = new EntityTargetLivingEntityEvent(this.getBukkitEntity(), ctarget, reason);
            Bukkit.getPluginManager().callEvent(event);
            if (event.isCancelled()) {
                arclight$targetSuccess = false;
                return;
            }
            if (event.getTarget() != null) {
                livingEntity = ((CraftLivingEntity) event.getTarget()).getHandle();
            } else {
                livingEntity = null;
            }
        }
        this.attackTarget = livingEntity;
        ForgeHooks.onLivingSetAttackTarget((MobEntity) (Object) this, this.attackTarget);
        arclight$targetSuccess = true;
    }

    public boolean setGoalTarget(LivingEntity livingEntity, EntityTargetEvent.TargetReason reason, boolean fireEvent) {
        bridge$pushGoalTargetReason(reason, fireEvent);
        setAttackTarget(livingEntity);
        return arclight$targetSuccess;
    }

    @Override
    public boolean bridge$lastGoalTargetResult() {
        return arclight$targetSuccess;
    }

    @Override
    public boolean bridge$setGoalTarget(LivingEntity livingEntity, EntityTargetEvent.TargetReason reason, boolean fireEvent) {
        return setGoalTarget(livingEntity, reason, fireEvent);
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

    @Inject(method = "writeAdditional", at = @At("HEAD"))
    private void arclight$setAware(CompoundNBT compound, CallbackInfo ci) {
        compound.putBoolean("Bukkit.Aware", this.aware);
    }

    @Inject(method = "readAdditional", at = @At("HEAD"))
    private void arclight$readAware(CompoundNBT compound, CallbackInfo ci) {
        if (compound.contains("Bukkit.Aware")) {
            this.aware = compound.getBoolean("Bukkit.Aware");
        }
    }

    @Redirect(method = "readAdditional", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/MobEntity;setCanPickUpLoot(Z)V"))
    public void arclight$setIfTrue(MobEntity mobEntity, boolean canPickup) {
        if (canPickup) mobEntity.setCanPickUpLoot(true);
    }

    @Redirect(method = "readAdditional", at = @At(value = "INVOKE", ordinal = 1, target = "Lnet/minecraft/nbt/CompoundNBT;getBoolean(Ljava/lang/String;)Z"))
    public boolean arclight$setIfTrue(CompoundNBT nbt, String key) {
        return nbt.getBoolean(key) || this.persistenceRequired;
    }

    @Inject(method = "updateEntityActionState", cancellable = true, at = @At("HEAD"))
    private void arclight$unaware(CallbackInfo ci) {
        if (!this.aware) {
            ++this.idleTime;
            ci.cancel();
        }
    }

    @Inject(method = "updateEquipmentIfNeeded", at = @At("HEAD"))
    private void arclight$captureItemEntity(ItemEntity itemEntity, CallbackInfo ci) {
        arclight$item = itemEntity;
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
    public boolean func_233665_g_(ItemStack p_233665_1_) {
        ItemEntity itemEntity = arclight$item;
        arclight$item = null;
        EquipmentSlotType equipmentslottype = getSlotForItemStack(p_233665_1_);
        ItemStack itemstack = this.getItemStackFromSlot(equipmentslottype);
        boolean flag = this.shouldExchangeEquipment(p_233665_1_, itemstack);
        boolean canPickup = flag && this.canEquipItem(p_233665_1_);
        if (itemEntity != null) {
            canPickup = !CraftEventFactory.callEntityPickupItemEvent((MobEntity) (Object) this, itemEntity, 0, !canPickup).isCancelled();
        }
        if (canPickup) {
            double d0 = this.getDropChance(equipmentslottype);
            if (!itemstack.isEmpty() && (double) Math.max(this.rand.nextFloat() - 0.1F, 0.0F) < d0) {
                forceDrops = true;
                this.entityDropItem(itemstack);
                forceDrops = false;
            }

            this.func_233657_b_(equipmentslottype, p_233665_1_);
            this.playEquipSound(p_233665_1_);
            return true;
        } else {
            return false;
        }
    }

    @Redirect(method = "checkDespawn", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/MobEntity;canDespawn(D)Z"))
    public boolean arclight$checkDespawn(MobEntity mobEntity, double distanceToClosestPlayer) {
        return true;
    }

    @Inject(method = "processInitialInteract", cancellable = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/MobEntity;clearLeashed(ZZ)V"))
    private void arclight$unleash(PlayerEntity player, Hand hand, CallbackInfoReturnable<ActionResultType> cir) {
        if (CraftEventFactory.callPlayerUnleashEntityEvent((MobEntity) (Object) this, player).isCancelled()) {
            ((ServerPlayerEntity) player).connection.sendPacket(new SMountEntityPacket((MobEntity) (Object) this, this.getLeashHolder()));
            cir.setReturnValue(ActionResultType.PASS);
        }
    }

    @Inject(method = "func_233661_c_", cancellable = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/MobEntity;setLeashHolder(Lnet/minecraft/entity/Entity;Z)V"))
    private void arclight$leash(PlayerEntity player, Hand hand, CallbackInfoReturnable<ActionResultType> cir) {
        if (CraftEventFactory.callPlayerLeashEntityEvent((MobEntity) (Object) this, player, player).isCancelled()) {
            ((ServerPlayerEntity) player).connection.sendPacket(new SMountEntityPacket((MobEntity) (Object) this, this.getLeashHolder()));
            cir.setReturnValue(ActionResultType.PASS);
        }
    }

    @Inject(method = "updateLeashedState", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/MobEntity;clearLeashed(ZZ)V"))
    public void arclight$unleash2(CallbackInfo ci) {
        Bukkit.getPluginManager().callEvent(new EntityUnleashEvent(this.getBukkitEntity(), this.isAlive() ?
            EntityUnleashEvent.UnleashReason.HOLDER_GONE : EntityUnleashEvent.UnleashReason.PLAYER_UNLEASH));
    }

    @Inject(method = "clearLeashed", at = @At(value = "INVOKE", shift = At.Shift.AFTER, target = "Lnet/minecraft/entity/MobEntity;entityDropItem(Lnet/minecraft/util/IItemProvider;)Lnet/minecraft/entity/item/ItemEntity;"))
    public void arclight$leashDropPost(boolean sendPacket, boolean dropLead, CallbackInfo ci) {
        this.forceDrops = false;
    }

    @Inject(method = "clearLeashed", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/MobEntity;entityDropItem(Lnet/minecraft/util/IItemProvider;)Lnet/minecraft/entity/item/ItemEntity;"))
    public void arclight$leashDropPre(boolean sendPacket, boolean dropLead, CallbackInfo ci) {
        this.forceDrops = true;
    }

    @Inject(method = "startRiding", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/MobEntity;clearLeashed(ZZ)V"))
    private void arclight$unleashRide(Entity entityIn, boolean force, CallbackInfoReturnable<Boolean> cir) {
        Bukkit.getPluginManager().callEvent(new EntityUnleashEvent(this.getBukkitEntity(), EntityUnleashEvent.UnleashReason.UNKNOWN));
    }

    @Inject(method = "setDead", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/MobEntity;clearLeashed(ZZ)V"))
    private void arclight$unleashDead(CallbackInfo ci) {
        Bukkit.getPluginManager().callEvent(new EntityUnleashEvent(this.getBukkitEntity(), EntityUnleashEvent.UnleashReason.UNKNOWN));
    }

    @Eject(method = "func_233656_b_", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;addEntity(Lnet/minecraft/entity/Entity;)Z"))
    private boolean arclight$copySpawn(World world, Entity entityIn, CallbackInfoReturnable<MobEntity> cir) {
        EntityTransformEvent.TransformReason transformReason = arclight$transform == null ? EntityTransformEvent.TransformReason.UNKNOWN : arclight$transform;
        arclight$transform = null;
        if (CraftEventFactory.callEntityTransformEvent((MobEntity) (Object) this, (LivingEntity) entityIn, transformReason).isCancelled()) {
            cir.setReturnValue(null);
            return false;
        } else {
            return world.addEntity(entityIn);
        }
    }

    @Inject(method = "func_233656_b_", at = @At("RETURN"))
    private <T extends MobEntity> void arclight$cleanReason(EntityType<T> p_233656_1_, boolean p_233656_2_, CallbackInfoReturnable<T> cir) {
        ((WorldBridge) this.world).bridge$pushAddEntityReason(null);
        this.arclight$transform = null;
    }

    public <T extends MobEntity> T a(EntityType<T> entityType, boolean flag, EntityTransformEvent.TransformReason transformReason, CreatureSpawnEvent.SpawnReason spawnReason) {
        ((WorldBridge) this.world).bridge$pushAddEntityReason(spawnReason);
        bridge$pushTransformReason(transformReason);
        return this.func_233656_b_(entityType, flag);
    }

    private transient EntityTransformEvent.TransformReason arclight$transform;

    @Override
    public void bridge$pushTransformReason(EntityTransformEvent.TransformReason transformReason) {
        this.arclight$transform = transformReason;
    }

    @Redirect(method = "attackEntityAsMob", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;setFire(I)V"))
    public void arclight$attackCombust(Entity entity, int seconds) {
        EntityCombustByEntityEvent combustEvent = new EntityCombustByEntityEvent(this.getBukkitEntity(), ((EntityBridge) entity).bridge$getBukkitEntity(), seconds);
        org.bukkit.Bukkit.getPluginManager().callEvent(combustEvent);
        if (!combustEvent.isCancelled()) {
            ((EntityBridge) entity).bridge$setOnFire(combustEvent.getDuration(), false);
        }
    }

    public ResourceLocation hack$getLootTable() {
        return this.shadow$getLootTable();
    }

    @Override
    public ResourceLocation bridge$getLootTable() {
        return this.hack$getLootTable();
    }

    @Override
    public boolean bridge$isPersistenceRequired() {
        return this.persistenceRequired;
    }

    @Override
    public void bridge$setPersistenceRequired(boolean value) {
        this.persistenceRequired = value;
    }
}
