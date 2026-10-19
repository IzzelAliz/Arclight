package io.izzel.arclight.common.mixin.core.world.level.block.entity;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.izzel.arclight.common.bridge.core.world.entity.LivingEntityBridge;
import io.izzel.arclight.common.bridge.core.world.damagesource.DamageSourceBridge;
import io.izzel.arclight.common.mod.mixins.annotation.TransformAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageSources;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.ConduitBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.craftbukkit.block.CraftBlock;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;
import java.util.Objects;

@Mixin(ConduitBlockEntity.class)
public abstract class ConduitBlockEntityMixin extends BlockEntityMixin {

    @Shadow
    private static EntityReference<LivingEntity> updateDestroyTarget(EntityReference<LivingEntity> target, ServerLevel level, BlockPos pos, boolean isActive) {
        throw new AssertionError();
    }

    @Shadow
    private static void updateAndAttackTarget(ServerLevel level, BlockPos pos, BlockState state, ConduitBlockEntity entity, boolean isActive) {
        throw new AssertionError();
    }

    @WrapOperation(method = "applyEffects", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;addEffect(Lnet/minecraft/world/effect/MobEffectInstance;)Z"))
    private static boolean arclight$addEntity(Player player, MobEffectInstance eff, Operation<Boolean> original) {
        var bridge = (LivingEntityBridge) player;
        var previous = bridge.bridge$getEffectCause().orElse(null);
        bridge.bridge$pushEffectCause(EntityPotionEffectEvent.Cause.CONDUIT);
        try {
            return original.call(player, eff);
        } finally {
            bridge.bridge$pushEffectCause(previous);
        }
    }

    @WrapOperation(method = "updateAndAttackTarget(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/entity/ConduitBlockEntity;Z)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/damagesource/DamageSources;magic()Lnet/minecraft/world/damagesource/DamageSource;"))
    private static DamageSource arclight$attackReason(DamageSources instance, Operation<DamageSource> original, ServerLevel level, BlockPos pos) {
        return ((DamageSourceBridge) original.call(instance)).bridge$directBlock(CraftBlock.at(level, pos));
    }

    // Fixed CraftConduit.setTarget links this exact six-argument static ABI with damageTarget=false.
    // Keep the existing native damage/sound policy for true; its fixed-CB difference needs review.
    // Merge privately; postApply publishes the exact public static CraftConduit ABI.
    @TransformAccess(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC)
    private static void updateAndAttackTarget(ServerLevel level, BlockPos pos, BlockState state, ConduitBlockEntity entity, boolean isActive, boolean damageTarget) {
        if (damageTarget) {
            updateAndAttackTarget(level, pos, state, entity, isActive);
            return;
        }
        EntityReference<LivingEntity> target = updateDestroyTarget(entity.destroyTarget, level, pos, isActive);
        if (!Objects.equals(target, entity.destroyTarget)) {
            entity.destroyTarget = target;
            level.sendBlockUpdated(pos, state, state, 2);
        }
    }

    @TransformAccess(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC)
    private static int getRange(List<BlockPos> list) {
        int i = list.size();
        return i / 7 * 16;
    }
}
