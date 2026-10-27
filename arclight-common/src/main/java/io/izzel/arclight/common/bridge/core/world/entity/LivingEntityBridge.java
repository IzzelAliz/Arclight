package io.izzel.arclight.common.bridge.core.world.entity;

import io.izzel.arclight.common.bridge.core.entity.EntityBridge;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.bukkit.craftbukkit.entity.CraftLivingEntity;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityKnockbackEvent;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;

import javax.annotation.Nullable;
import java.util.Optional;

public interface LivingEntityBridge extends EntityBridge {

    org.bukkit.craftbukkit.attribute.CraftAttributeMap bridge$getCraftAttributes();

    void bridge$setSlot(EquipmentSlot slotIn, ItemStack stack, boolean silent);

    void bridge$playEquipSound(EquipmentSlot slot, ItemStack oldItem, ItemStack newItem, boolean silent);

    boolean bridge$canPickUpLoot();

    int bridge$getExpReward(Entity entity);

    void bridge$setExpToDrop(int amount);

    int bridge$getExpToDrop();

    int bridge$getInvulnerableDuration();

    void bridge$setInvulnerableDuration(int ticks);

    void bridge$pushHealReason(EntityRegainHealthEvent.RegainReason regainReason);

    /** Reads the pending one-shot reason without consuming it; null means none. */
    @Nullable
    EntityRegainHealthEvent.RegainReason bridge$peekHealReason();

    void bridge$heal(float healAmount, EntityRegainHealthEvent.RegainReason regainReason);

    void bridge$pushEffectCause(EntityPotionEffectEvent.Cause cause);

    boolean bridge$addEffect(MobEffectInstance effect, EntityPotionEffectEvent.Cause cause);

    boolean bridge$removeEffect(Holder<MobEffect> effect, EntityPotionEffectEvent.Cause cause);

    boolean bridge$removeAllEffects(EntityPotionEffectEvent.Cause cause);

    Optional<EntityPotionEffectEvent.Cause> bridge$getEffectCause();

    /** Fires the one Bukkit pre-removal decision for a bulk-clear candidate. */
    boolean bridge$willRemoveEffect(MobEffectInstance effect);

    void bridge$pushKnockbackCause(Entity attacker, EntityKnockbackEvent.KnockbackCause cause);

    @Override
    default CraftLivingEntity getBukkitEntity() {
        return bridge$getBukkitEntity();
    }

    @Override
    CraftLivingEntity bridge$getBukkitEntity();

    default int bridge$forge$getExperienceDrop(LivingEntity entity, Player attackingPlayer, int originalExperience) {
        return originalExperience;
    }

    default boolean bridge$forge$onLivingUseTotem(LivingEntity entity, DamageSource damageSource, ItemStack totem, InteractionHand hand) {
        return true;
    }

    enum LivingTargetType {
        BEHAVIOR_TARGET,
        MOB_TARGET
    }

    default void bridge$forge$onLivingConvert(LivingEntity entity, LivingEntity outcome) {}

    default boolean bridge$forge$canEntityDestroy(Level level, BlockPos pos, LivingEntity entity) {
        return true;
    }

    @Nullable
    EntityDamageEvent arclight$fireEntityDamageEvent(DamageSource source, float original);

    default org.bukkit.craftbukkit.entity.CraftLivingEntity getBukkitLivingEntity() {
        return this.bridge$getBukkitEntity();
    }

    default void heal(float amount, org.bukkit.event.entity.EntityRegainHealthEvent.RegainReason reason) {
        this.bridge$heal(amount, reason);
    }

    default boolean addEffect(net.minecraft.world.effect.MobEffectInstance effect, org.bukkit.event.entity.EntityPotionEffectEvent.Cause cause) {
        return this.bridge$addEffect(effect, cause);
    }

    default boolean removeEffect(net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> effect, org.bukkit.event.entity.EntityPotionEffectEvent.Cause cause) {
        return this.bridge$removeEffect(effect, cause);
    }

    default boolean removeAllEffects(org.bukkit.event.entity.EntityPotionEffectEvent.Cause cause) {
        return this.bridge$removeAllEffects(cause);
    }

    default void setItemSlot(net.minecraft.world.entity.EquipmentSlot slot, net.minecraft.world.item.ItemStack stack, boolean silent) {
        this.bridge$setSlot(slot, stack, silent);
    }

}
