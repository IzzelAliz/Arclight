package io.izzel.arclight.common.mixin.core.entity.projectile;

import io.izzel.arclight.common.bridge.entity.LivingEntityBridge;
import io.izzel.arclight.common.bridge.world.WorldBridge;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CampfireBlock;
import net.minecraft.entity.AreaEffectCloudEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.entity.projectile.PotionEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Potion;
import net.minecraft.util.Direction;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
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

@Mixin(PotionEntity.class)
public abstract class PotionEntityMixin extends ProjectileItemEntityMixin {

    @Redirect(method = "onImpact", at = @At(value = "INVOKE", remap = false, ordinal = 1, target = "Ljava/util/List;isEmpty()Z"))
    private boolean arclight$callEvent(List list) {
        return false;
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    private void func_213888_a(List<EffectInstance> list, @Nullable Entity entity) {
        AxisAlignedBB axisalignedbb = this.getBoundingBox().grow(4.0, 2.0, 4.0);
        List<LivingEntity> list2 = this.world.getEntitiesWithinAABB(LivingEntity.class, axisalignedbb);
        Map<org.bukkit.entity.LivingEntity, Double> affected = new HashMap<>();
        if (!list2.isEmpty()) {
            for (LivingEntity entityliving : list2) {
                if (entityliving.canBeHitWithPotion()) {
                    double d0 = this.getDistanceSq(entityliving);
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
        PotionSplashEvent event = CraftEventFactory.callPotionSplashEvent((PotionEntity) (Object) this, affected);
        if (!event.isCancelled() && list != null && !list.isEmpty()) {
            for (org.bukkit.entity.LivingEntity victim : event.getAffectedEntities()) {
                if (!(victim instanceof CraftLivingEntity)) {
                    continue;
                }
                LivingEntity entityliving2 = ((CraftLivingEntity) victim).getHandle();
                double d2 = event.getIntensity(victim);
                for (EffectInstance mobeffect : list) {
                    Effect mobeffectlist = mobeffect.getPotion();
                    if (!((WorldBridge) this.world).bridge$isPvpMode() && this.func_234616_v_() instanceof ServerPlayerEntity && entityliving2 instanceof ServerPlayerEntity && entityliving2 != this.func_234616_v_()) {
                        int i = Effect.getId(mobeffectlist);
                        if (i == 2 || i == 4 || i == 7 || i == 15 || i == 17 || i == 18) {
                            continue;
                        }
                        if (i == 19) {
                            continue;
                        }
                    }
                    if (mobeffectlist.isInstant()) {
                        mobeffectlist.affectEntity((PotionEntity) (Object) this, this.func_234616_v_(), entityliving2, mobeffect.getAmplifier(), d2);
                    } else {
                        int i = (int) (d2 * mobeffect.getDuration() + 0.5);
                        if (i <= 20) {
                            continue;
                        }
                        ((LivingEntityBridge) entityliving2).bridge$pushEffectCause(EntityPotionEffectEvent.Cause.POTION_SPLASH);
                        entityliving2.addPotionEffect(new EffectInstance(mobeffectlist, i, mobeffect.getAmplifier(), mobeffect.isAmbient(), mobeffect.doesShowParticles()));
                    }
                }
            }
        }
    }

    @Inject(method = "makeAreaOfEffectCloud", cancellable = true, locals = LocalCapture.CAPTURE_FAILHARD, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;addEntity(Lnet/minecraft/entity/Entity;)Z"))
    private void arclight$makeCloud(ItemStack p_190542_1_, Potion p_190542_2_, CallbackInfo ci, AreaEffectCloudEntity entity) {
        LingeringPotionSplashEvent event = CraftEventFactory.callLingeringPotionSplashEvent((PotionEntity) (Object) this, entity);
        if (event.isCancelled() || entity.removed) {
            ci.cancel();
            entity.remove();
        }
    }

    @Inject(method = "extinguishFires", cancellable = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;removeBlock(Lnet/minecraft/util/math/BlockPos;Z)Z"))
    private void arclight$entityChangeBlock(BlockPos pos, Direction direction, CallbackInfo ci) {
        if (CraftEventFactory.callEntityChangeBlockEvent((PotionEntity) (Object) this, pos.offset(direction), Blocks.AIR.getDefaultState()).isCancelled()) {
            ci.cancel();
        }
    }

    @Inject(method = "extinguishFires", cancellable = true, locals = LocalCapture.CAPTURE_FAILHARD, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;playEvent(Lnet/minecraft/entity/player/PlayerEntity;ILnet/minecraft/util/math/BlockPos;I)V"))
    private void arclight$entityChangeBlock2(BlockPos pos, Direction p_184542_2_, CallbackInfo ci, BlockState state) {
        if (CraftEventFactory.callEntityChangeBlockEvent((PotionEntity) (Object) this, pos, state.with(CampfireBlock.LIT, false)).isCancelled()) {
            ci.cancel();
        }
    }
}
