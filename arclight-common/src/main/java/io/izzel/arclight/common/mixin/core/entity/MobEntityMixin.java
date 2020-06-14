package io.izzel.arclight.common.mixin.core.entity;

import io.izzel.arclight.common.bridge.entity.EntityBridge;
import io.izzel.arclight.common.bridge.entity.LivingEntityBridge;
import io.izzel.arclight.common.bridge.entity.MobEntityBridge;
import io.izzel.arclight.common.mod.ArclightMod;
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
import net.minecraft.util.Hand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeHooks;
import org.apache.logging.log4j.Level;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v.entity.CraftLivingEntity;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.bukkit.event.entity.EntityCombustByEntityEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
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
import java.util.concurrent.atomic.AtomicBoolean;

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
    @Shadow protected abstract boolean shouldExchangeEquipment(ItemStack candidate, ItemStack existing, EquipmentSlotType p_208003_3_);
    @Shadow protected abstract boolean canEquipItem(ItemStack stack);
    @Shadow protected abstract float getDropChance(EquipmentSlotType slotIn);
    @Shadow public abstract void setItemStackToSlot(EquipmentSlotType slotIn, ItemStack stack);
    @Shadow @Final public float[] inventoryHandsDropChances;
    @Shadow @Final public float[] inventoryArmorDropChances;
    @Shadow @Nullable public abstract Entity getLeashHolder();
    @Shadow public abstract boolean isNoDespawnRequired();
    @Shadow protected void updateAITasks() { }
    @Shadow public abstract boolean isAIDisabled();
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

    private transient AtomicBoolean arclight$targetSuccess;
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
        if (fireEvent) {
            if (reason == EntityTargetEvent.TargetReason.UNKNOWN && this.getAttackTarget() != null && livingEntity == null) {
                reason = (this.getAttackTarget().isAlive() ? EntityTargetEvent.TargetReason.FORGOT_TARGET : EntityTargetEvent.TargetReason.TARGET_DIED);
            }
            if (reason == EntityTargetEvent.TargetReason.UNKNOWN) {
                ArclightMod.LOGGER.log(Level.WARN, "Unknown target reason, please report on the issue tracker", new Exception());
            }
            CraftLivingEntity ctarget = null;
            if (livingEntity != null) {
                ctarget = ((LivingEntityBridge) livingEntity).bridge$getBukkitEntity();
            }
            final EntityTargetLivingEntityEvent event = new EntityTargetLivingEntityEvent(this.getBukkitEntity(), ctarget, reason);
            Bukkit.getPluginManager().callEvent(event);
            if (event.isCancelled()) {
                if (arclight$targetSuccess != null) arclight$targetSuccess.set(false);
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
        if (arclight$targetSuccess != null) arclight$targetSuccess.set(true);
    }

    public boolean setGoalTarget(LivingEntity livingEntity, EntityTargetEvent.TargetReason reason, boolean fireEvent) {
        bridge$pushGoalTargetReason(reason, fireEvent);
        if (getAttackTarget() == livingEntity) {
            return false;
        } else {
            arclight$targetSuccess = new AtomicBoolean();
            setAttackTarget(livingEntity);
            boolean ret = arclight$targetSuccess.get();
            arclight$targetSuccess = null;
            return ret;
        }
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

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    protected void updateEquipmentIfNeeded(ItemEntity itemEntity) {
        ItemStack itemstack = itemEntity.getItem();
        EquipmentSlotType equipmentslottype = getSlotForItemStack(itemstack);
        ItemStack itemstack1 = this.getItemStackFromSlot(equipmentslottype);
        boolean flag = this.shouldExchangeEquipment(itemstack, itemstack1, equipmentslottype);
        boolean canPickup = flag && this.canEquipItem(itemstack);
        canPickup = !CraftEventFactory.callEntityPickupItemEvent((Entity) (Object) this, itemEntity, 0, !canPickup).isCancelled();
        if (canPickup) {
            double d0 = this.getDropChance(equipmentslottype);
            if (!itemstack1.isEmpty() && (double) (this.rand.nextFloat() - 0.1F) < d0) {
                forceDrops = true;
                this.entityDropItem(itemstack1);
                forceDrops = false;
            }

            this.setItemStackToSlot(equipmentslottype, itemstack);
            switch (equipmentslottype.getSlotType()) {
                case HAND:
                    this.inventoryHandsDropChances[equipmentslottype.getIndex()] = 2.0F;
                    break;
                case ARMOR:
                    this.inventoryArmorDropChances[equipmentslottype.getIndex()] = 2.0F;
            }

            this.persistenceRequired = true;
            this.onItemPickup(itemEntity, itemstack.getCount());
            itemEntity.remove();
        }
    }

    @Redirect(method = "checkDespawn", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/MobEntity;canDespawn(D)Z"))
    public boolean arclight$checkDespawn(MobEntity mobEntity, double distanceToClosestPlayer) {
        return true;
    }

    @Inject(method = "processInitialInteract", cancellable = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/MobEntity;clearLeashed(ZZ)V"))
    private void arclight$unleash(PlayerEntity player, Hand hand, CallbackInfoReturnable<Boolean> cir) {
        if (CraftEventFactory.callPlayerUnleashEntityEvent((MobEntity) (Object) this, player).isCancelled()) {
            ((ServerPlayerEntity) player).connection.sendPacket(new SMountEntityPacket((MobEntity) (Object) this, this.getLeashHolder()));
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "processInitialInteract", cancellable = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/MobEntity;setLeashHolder(Lnet/minecraft/entity/Entity;Z)V"))
    private void arclight$leash(PlayerEntity player, Hand hand, CallbackInfoReturnable<Boolean> cir) {
        if (CraftEventFactory.callPlayerLeashEntityEvent((MobEntity) (Object) this, player, player).isCancelled()) {
            ((ServerPlayerEntity) player).connection.sendPacket(new SMountEntityPacket((MobEntity) (Object) this, this.getLeashHolder()));
            cir.setReturnValue(false);
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

    @Inject(method = "recreateLeash", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/MobEntity;clearLeashed(ZZ)V"))
    public void arclight$createLeash(CallbackInfo ci) {
        Bukkit.getPluginManager().callEvent(new EntityUnleashEvent(this.getBukkitEntity(), EntityUnleashEvent.UnleashReason.UNKNOWN));
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
