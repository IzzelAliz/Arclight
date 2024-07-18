package io.izzel.arclight.common.mixin.core.world.entity.projectile;

import io.izzel.arclight.common.bridge.core.entity.LivingEntityBridge;
import io.izzel.arclight.common.bridge.core.world.WorldBridge;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrownPotion;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.level.block.AbstractCandleBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.bukkit.craftbukkit.v.entity.CraftLivingEntity;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.event.entity.LingeringPotionSplashEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Mixin(ThrownPotion.class)
public abstract class ThrownPotionMixin extends ThrowableItemProjectileMixin {

    @Redirect(method = "onHit", at = @At(value = "INVOKE", remap = false, ordinal = 1, target = "Ljava/util/List;isEmpty()Z"))
    private boolean arclight$callEvent(List list) {
        return false;
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    private void applySplash(List<MobEffectInstance> list, @Nullable Entity entity) {
        AABB axisalignedbb = this.getBoundingBox().inflate(4.0, 2.0, 4.0);
        List<LivingEntity> list2 = this.level().getEntitiesOfClass(LivingEntity.class, axisalignedbb);
        Map<org.bukkit.entity.LivingEntity, Double> affected = new HashMap<>();
        if (!list2.isEmpty()) {
            for (LivingEntity entityliving : list2) {
                if (entityliving.isAffectedByPotions()) {
                    double d0 = this.distanceToSqr(entityliving);
                    if (d0 >= 16.0) {
                        continue;
                    }
                    double d2 = 1.0 - Math.sqrt(d0) / 4.0;
                    if (entityliving == entity) {
                        d2 = 1.0;
                    }
                    affected.put(((LivingEntityBridge) entityliving).bridge$getBukkitEntity(), d2);
                }
            }
        }
        PotionSplashEvent event = CraftEventFactory.callPotionSplashEvent((ThrownPotion) (Object) this, affected);
        if (!event.isCancelled() && list != null && !list.isEmpty()) {
            for (org.bukkit.entity.LivingEntity victim : event.getAffectedEntities()) {
                if (!(victim instanceof CraftLivingEntity)) {
                    continue;
                }
                LivingEntity entityliving2 = ((CraftLivingEntity) victim).getHandle();
                double d2 = event.getIntensity(victim);
                for (MobEffectInstance mobeffect : list) {
                    MobEffect mobeffectlist = mobeffect.getEffect();
                    if (!((WorldBridge) this.level()).bridge$isPvpMode() && this.getOwner() instanceof ServerPlayer && entityliving2 instanceof ServerPlayer && entityliving2 != this.getOwner()) {
                        int i = MobEffect.getId(mobeffectlist);
                        if (i == 2 || i == 4 || i == 7 || i == 15 || i == 17 || i == 18) {
                            continue;
                        }
                        if (i == 19) {
                            continue;
                        }
                    }
                    if (mobeffectlist.isInstantenous()) {
                        mobeffectlist.applyInstantenousEffect((ThrownPotion) (Object) this, this.getOwner(), entityliving2, mobeffect.getAmplifier(), d2);
                    } else {
                        int i = (int) (d2 * mobeffect.getDuration() + 0.5);
                        if (i <= 20) {
                            continue;
                        }
                        ((LivingEntityBridge) entityliving2).bridge$pushEffectCause(EntityPotionEffectEvent.Cause.POTION_SPLASH);
                        entityliving2.addEffect(new MobEffectInstance(mobeffectlist, i, mobeffect.getAmplifier(), mobeffect.isAmbient(), mobeffect.isVisible()), (Entity) null);
                    }
                }
            }
        }
    }

    @Inject(method = "makeAreaOfEffectCloud", cancellable = true, locals = LocalCapture.CAPTURE_FAILHARD, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"))
    private void arclight$makeCloud(ItemStack p_190542_1_, Potion p_190542_2_, CallbackInfo ci, AreaEffectCloud entity) {
        LingeringPotionSplashEvent event = CraftEventFactory.callLingeringPotionSplashEvent((ThrownPotion) (Object) this, entity);
        if (event.isCancelled() || entity.isRemoved()) {
            ci.cancel();
            entity.discard();
        }
    }

    @Inject(method = "dowseFire", cancellable = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;removeBlock(Lnet/minecraft/core/BlockPos;Z)Z"))
    private void arclight$entityChangeBlock(BlockPos pos, CallbackInfo ci) {
        if (!CraftEventFactory.callEntityChangeBlockEvent((ThrownPotion) (Object) this, pos, Blocks.AIR.defaultBlockState())) {
            ci.cancel();
        }
    }

    @Inject(method = "dowseFire", cancellable = true, locals = LocalCapture.CAPTURE_FAILHARD, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;levelEvent(Lnet/minecraft/world/entity/player/Player;ILnet/minecraft/core/BlockPos;I)V"))
    private void arclight$entityChangeBlock2(BlockPos pos, CallbackInfo ci, BlockState state) {
        if (!CraftEventFactory.callEntityChangeBlockEvent((ThrownPotion) (Object) this, pos, state.setValue(CampfireBlock.LIT, false))) {
            ci.cancel();
        }
    }

    @Inject(method = "dowseFire", cancellable = true, locals = LocalCapture.CAPTURE_FAILHARD, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/AbstractCandleBlock;extinguish(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/LevelAccessor;Lnet/minecraft/core/BlockPos;)V"))
    private void arclight$entityChangeBlock3(BlockPos pos, CallbackInfo ci, BlockState state) {
        if (!CraftEventFactory.callEntityChangeBlockEvent((ThrownPotion) (Object) this, pos, state.setValue(AbstractCandleBlock.LIT, false))) {
            ci.cancel();
        }
    }
}
