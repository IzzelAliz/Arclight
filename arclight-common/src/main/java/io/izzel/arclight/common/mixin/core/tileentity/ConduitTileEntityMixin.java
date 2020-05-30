package io.izzel.arclight.common.mixin.core.tileentity;

import io.izzel.arclight.common.bridge.entity.player.PlayerEntityBridge;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.tileentity.ConduitTileEntity;
import net.minecraft.util.math.AxisAlignedBB;
import org.bukkit.craftbukkit.v.block.CraftBlock;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.Iterator;
import java.util.List;

@Mixin(ConduitTileEntity.class)
public abstract class ConduitTileEntityMixin extends TileEntityMixin {

    @Inject(method = "addEffectsToPlayers", locals = LocalCapture.CAPTURE_FAILHARD, at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerEntity;addPotionEffect(Lnet/minecraft/potion/EffectInstance;)Z"))
    public void arclight$addEntity(CallbackInfo ci, int i, int j, int k, int l, int i1, AxisAlignedBB bb, List<?> list,
                                   Iterator<?> iterator, PlayerEntity playerEntity) {
        ((PlayerEntityBridge) playerEntity).bridge$pushEffectCause(EntityPotionEffectEvent.Cause.CONDUIT);
    }

    @Inject(method = "attackMobs", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;attackEntityFrom(Lnet/minecraft/util/DamageSource;F)Z"))
    public void arclight$attackReason(CallbackInfo ci) {
        CraftEventFactory.blockDamage = CraftBlock.at(this.world, this.pos);
    }

    @Inject(method = "attackMobs", at = @At(value = "INVOKE", shift = At.Shift.AFTER, target = "Lnet/minecraft/entity/LivingEntity;attackEntityFrom(Lnet/minecraft/util/DamageSource;F)Z"))
    public void arclight$attackReasonReset(CallbackInfo ci) {
        CraftEventFactory.blockDamage = null;
    }
}
