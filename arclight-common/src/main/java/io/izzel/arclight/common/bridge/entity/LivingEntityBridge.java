package io.izzel.arclight.common.bridge.entity;

import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectInstance;
import org.bukkit.craftbukkit.v.entity.CraftLivingEntity;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;

import java.util.Optional;

public interface LivingEntityBridge extends EntityBridge {

    boolean bridge$canPickUpLoot();

    boolean bridge$isForceDrops();

    int bridge$getExpReward();

    void bridge$setExpToDrop(int amount);

    int bridge$getExpToDrop();

    void bridge$pushHealReason(EntityRegainHealthEvent.RegainReason regainReason);

    void bridge$heal(float healAmount, EntityRegainHealthEvent.RegainReason regainReason);

    void bridge$pushEffectCause(EntityPotionEffectEvent.Cause cause);

    boolean bridge$addEffect(EffectInstance effect, EntityPotionEffectEvent.Cause cause);

    boolean bridge$removeEffect(Effect effect, EntityPotionEffectEvent.Cause cause);

    boolean bridge$removeAllEffects(EntityPotionEffectEvent.Cause cause);

    Optional<EntityPotionEffectEvent.Cause> bridge$getEffectCause();

    EntityPotionEffectEvent.Action bridge$getAndResetAction();

    @Override
    CraftLivingEntity bridge$getBukkitEntity();
}
