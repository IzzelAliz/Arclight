package io.izzel.arclight.common.bridge.core.entity;

import io.izzel.tools.product.Product;
import io.izzel.tools.product.Product3;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.craftbukkit.v.entity.CraftLivingEntity;
import org.bukkit.event.entity.EntityKnockbackEvent;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Optional;

public interface LivingEntityBridge extends EntityBridge {

    void bridge$setSlot(EquipmentSlot slotIn, ItemStack stack, boolean silent);

    void bridge$playEquipSound(EquipmentSlot slot, ItemStack oldItem, ItemStack newItem, boolean silent);

    boolean bridge$canPickUpLoot();

    boolean bridge$isForceDrops();

    int bridge$getExpReward();

    void bridge$setExpToDrop(int amount);

    int bridge$getExpToDrop();

    void bridge$pushHealReason(EntityRegainHealthEvent.RegainReason regainReason);

    void bridge$heal(float healAmount, EntityRegainHealthEvent.RegainReason regainReason);

    void bridge$pushEffectCause(EntityPotionEffectEvent.Cause cause);

    boolean bridge$addEffect(MobEffectInstance effect, EntityPotionEffectEvent.Cause cause);

    boolean bridge$removeEffect(MobEffect effect, EntityPotionEffectEvent.Cause cause);

    boolean bridge$removeAllEffects(EntityPotionEffectEvent.Cause cause);

    Optional<EntityPotionEffectEvent.Cause> bridge$getEffectCause();

    void bridge$pushKnockbackCause(Entity attacker, EntityKnockbackEvent.KnockbackCause cause);

    @Override
    CraftLivingEntity bridge$getBukkitEntity();

    default int bridge$forge$getExperienceDrop(LivingEntity entity, Player attackingPlayer, int originalExperience) {
        return originalExperience;
    }

    default boolean bridge$forge$mobEffectExpired(MobEffectInstance effect) {
        return false;
    }

    default boolean bridge$forge$mobEffectAdded(MobEffectInstance old, MobEffectInstance effect, Entity entity) {
        return false;
    }

    default float bridge$forge$onLivingHurt(LivingEntity entity, DamageSource src, float amount) {
        return amount;
    }

    default Product3<Boolean /*cancelled*/, Float /*block damage*/, Boolean /*shield take damage*/> bridge$forge$onShieldBlock(
        LivingEntity blocker, DamageSource source, float blocked) {
        return Product.of(false, blocked, true);
    }

    default float bridge$forge$onLivingDamage(LivingEntity entity, DamageSource src, float amount) {
        return amount;
    }

    default boolean bridge$forge$onLivingUseTotem(LivingEntity entity, DamageSource damageSource, ItemStack totem, InteractionHand hand) {
        return true;
    }

    enum LivingTargetType {
        BEHAVIOR_TARGET,
        MOB_TARGET
    }

    default @Nullable LivingEntity bridge$forge$onLivingChangeTarget(LivingEntity entity, LivingEntity originalTarget, LivingTargetType targetType) {
        return originalTarget;
    }

    default BlockPos bridge$forge$onEnderTeleport(LivingEntity entity, double targetX, double targetY, double targetZ) {
        return BlockPos.containing(targetX, targetY, targetZ);
    }

    default void bridge$forge$onLivingConvert(LivingEntity entity, LivingEntity outcome) {}

    default boolean bridge$forge$canEntityDestroy(Level level, BlockPos pos, LivingEntity entity) {
        return true;
    }

    default boolean bridge$forge$onEntityDestroyBlock(LivingEntity entity, BlockPos pos, BlockState state) {
        return true;
    }

    void bridge$common$startCaptureDrops();

    boolean bridge$common$isCapturingDrops();

    void bridge$common$captureDrop(ItemEntity itemEntity);

    Collection<ItemEntity> bridge$common$getCapturedDrops();

    void bridge$common$finishCaptureAndFireEvent();
}
