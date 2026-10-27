package io.izzel.arclight.common.mixin.core.world.entity.decoration;

import io.izzel.arclight.common.bridge.core.entity.EntityBridge;
import io.izzel.arclight.common.bridge.core.server.level.ServerPlayerBridge;
import io.izzel.arclight.common.mixin.core.world.entity.LivingEntityMixin;
import io.izzel.arclight.common.mod.util.ArclightCaptures;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.CraftEquipmentSlot;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityRemoveEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;

@Mixin(net.minecraft.world.entity.decoration.ArmorStand.class)
public abstract class ArmorStandMixin extends LivingEntityMixin {

    // @formatter:off
    @Shadow private boolean invisible;
    // @formatter:on

    @Override
    public float getBukkitYaw() {
        return this.getYRot();
    }

    @Inject(method = "hurtServer", cancellable = true, at = @At(value = "INVOKE", ordinal = 0, target = "Lnet/minecraft/world/entity/decoration/ArmorStand;kill(Lnet/minecraft/server/level/ServerLevel;)V"))
    public void arclight$damageDropOut(ServerLevel serverLevel, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (CraftEventFactory.handleNonLivingEntityDamageEvent((net.minecraft.world.entity.decoration.ArmorStand) (Object) this, source, amount)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "hurtServer", cancellable = true, at = @At(value = "FIELD", target = "Lnet/minecraft/tags/DamageTypeTags;IS_EXPLOSION:Lnet/minecraft/tags/TagKey;"))
    public void arclight$damageNormal(ServerLevel serverLevel, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (CraftEventFactory.handleNonLivingEntityDamageEvent((net.minecraft.world.entity.decoration.ArmorStand) (Object) this, source, amount, true, this.invisible)) {
            cir.setReturnValue(false);
        }
    }

    @Redirect(method = "hurtServer", at = @At(value = "FIELD", target = "Lnet/minecraft/world/entity/decoration/ArmorStand;invisible:Z"))
    private boolean arclight$softenCondition(net.minecraft.world.entity.decoration.ArmorStand entity) {
        return false;
    }

    private boolean arclight$preparedDeath;
    private net.minecraft.world.entity.item.ItemEntity arclight$standDrop;

    @Inject(method = "hurtServer", at = @At("HEAD"))
    private void arclight$resetDeath(ServerLevel level, DamageSource source, float damage, CallbackInfoReturnable<Boolean> cir) {
        this.arclight$preparedDeath = false;
        this.arclight$standDrop = null;
        ((io.izzel.arclight.common.bridge.core.world.entity.DeathEventBridge) this).bridge$resetDeathEvent();
    }

    @Inject(method = "kill", at = @At("HEAD"), cancellable = true)
    private void arclight$pushRemoveCause(ServerLevel level, CallbackInfo ci) {
        var death = (io.izzel.arclight.common.bridge.core.world.entity.DeathEventBridge) this;
        if (!this.arclight$preparedDeath) {
            death.bridge$resetDeathEvent();
            CraftEventFactory.callEntityDeathEvent((net.minecraft.world.entity.LivingEntity) (Object) this, this.damageSources().genericKill());
        }
        this.arclight$preparedDeath = false;
        if (death.bridge$deathCancelled()) {
            ci.cancel();
            return;
        }
        this.bridge$pushEntityRemoveCause(EntityRemoveEvent.Cause.DEATH);
    }

    @com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation(method = "brokenByPlayer", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/level/block/Block;popResource(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/item/ItemStack;)V"))
    private void arclight$captureStand(Level level, BlockPos pos, ItemStack stack,
            com.llamalad7.mixinextras.injector.wrapoperation.Operation<Void> original) {
        this.arclight$standDrop = this.arclight$spawnAtLocationNoAdd(stack);
    }

    @com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation(method = "brokenByAnything", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/entity/EntityEquipment;set(Lnet/minecraft/world/entity/EquipmentSlot;Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/world/item/ItemStack;"))
    private ItemStack arclight$deferClear(net.minecraft.world.entity.EntityEquipment equipment,
            net.minecraft.world.entity.EquipmentSlot slot, ItemStack stack,
            com.llamalad7.mixinextras.injector.wrapoperation.Operation<ItemStack> original) {
        ItemStack previous = equipment.get(slot);
        ((io.izzel.arclight.common.bridge.core.world.entity.DeathEventBridge) this)
            .bridge$deferDeathChange(() -> original.call(equipment, slot, stack));
        return previous;
    }

    @Redirect(method = "brokenByAnything", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/decoration/ArmorStand;dropAllDeathLoot(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;)V"))
    private void arclight$dropLater(net.minecraft.world.entity.decoration.ArmorStand instance, ServerLevel serverLevel, DamageSource damageSource) {
        var drops = new ArrayList<net.minecraft.world.entity.item.ItemEntity>(6);
        if (this.arclight$standDrop != null) {
            drops.add(this.arclight$standDrop);
            this.arclight$standDrop = null;
        }
        ArclightCaptures.captureExtraDrops(drops);
    }

    @Decorate(method = "brokenByAnything", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/Block;popResource(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/item/ItemStack;)V"))
    private void arclight$captureDropsDeath(Level worldIn, BlockPos pos, ItemStack stack) throws Throwable {
        try {
            ArclightCaptures.captureBlockPopResForExtraDrops(true);
            DecorationOps.callsite().invoke(worldIn, pos, stack);
        } finally {
            ArclightCaptures.captureBlockPopResForExtraDrops(false);
        }
    }

    @Inject(method = "brokenByAnything", at = @At("RETURN"))
    private void arclight$spawnLast(ServerLevel serverLevel, DamageSource damageSource, CallbackInfo ci) {
        this.dropAllDeathLoot(serverLevel, damageSource);
        this.arclight$preparedDeath = true;
    }

    @Override
    protected boolean shouldDropExperience() {
        return true;
    }


    @Inject(method = "swapItem", cancellable = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;hasInfiniteMaterials()Z"))
    public void arclight$manipulateEvent(net.minecraft.world.entity.player.Player playerEntity, net.minecraft.world.entity.EquipmentSlot slotType, ItemStack itemStack, InteractionHand hand, CallbackInfoReturnable<Boolean> cir) {
        ItemStack itemStack1 = this.getItemBySlot(slotType);

        org.bukkit.inventory.ItemStack armorStandItem = CraftItemStack.asCraftMirror(itemStack1);
        org.bukkit.inventory.ItemStack playerHeldItem = CraftItemStack.asCraftMirror(itemStack);

        Player player = ((ServerPlayerBridge) playerEntity).bridge$getBukkitEntity();
        ArmorStand self = (ArmorStand) ((EntityBridge) this).bridge$getBukkitEntity();

        EquipmentSlot slot = CraftEquipmentSlot.getSlot(slotType);
        EquipmentSlot bukkitHand = CraftEquipmentSlot.getHand(hand);
        PlayerArmorStandManipulateEvent event = new PlayerArmorStandManipulateEvent(player, self, playerHeldItem, armorStandItem, slot, bukkitHand);
        Bukkit.getPluginManager().callEvent(event);

        if (event.isCancelled()) {
            cir.setReturnValue(true);
        }
    }

    @Override
    public void setItemSlot(net.minecraft.world.entity.EquipmentSlot slot, ItemStack stack, boolean silent) {
        switch (slot.getType()) {
            case HAND, HUMANOID_ARMOR -> this.bridge$playEquipSound(slot, this.equipment.set(slot, stack), stack, silent);
        }
    }
}
