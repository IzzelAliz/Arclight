package io.izzel.arclight.common.mixin.vanilla.world.entity;

import com.google.common.base.Function;
import io.izzel.arclight.common.bridge.bukkit.EntityDamageEventBridge;
import io.izzel.arclight.common.mod.server.ArclightServer;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import io.izzel.arclight.mixin.Local;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.damagesource.CombatRules;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.bukkit.event.entity.EntityDamageEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import javax.annotation.Nullable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin_Vanilla extends EntityMixin_Vanilla {

    // @formatter:off
    @Shadow public abstract boolean isDamageSourceBlocked(DamageSource arg);
    @Shadow public abstract ItemStack getItemBySlot(EquipmentSlot arg);
    @Shadow public int invulnerableDuration;
    @Shadow public float lastHurt;
    @Shadow public abstract int getArmorValue();
    @Shadow public abstract double getAttributeValue(Holder<Attribute> arg);
    @Shadow public abstract boolean hasEffect(Holder<MobEffect> arg);
    @Shadow @Nullable public abstract MobEffectInstance getEffect(Holder<MobEffect> arg);
    @Shadow public abstract float getAbsorptionAmount();
    // @formatter:on

    protected transient EntityDamageEvent arclight$currentDamageEvent;

    @Decorate(method = "hurt", inject = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;isSleeping()Z"))
    private void arclight$entityDamageEvent(DamageSource damagesource, float originalDamage, @Local(allocate = "bukkitEvent") EntityDamageEvent event) throws Throwable {
        event = arclight$fireEvent(damagesource, originalDamage);

        if (event == null || event.isCancelled()) {
            DecorationOps.cancel().invoke(false);
            return;
        }

        if (damagesource.getEntity() instanceof net.minecraft.world.entity.player.Player) {
            ((net.minecraft.world.entity.player.Player) damagesource.getEntity()).resetAttackStrengthTicker();
        }
    }

    private EntityDamageEvent arclight$fireEvent(DamageSource source, float original) {
        float damage = original;

        Function<Double, Double> blocking = f -> -((this.isDamageSourceBlocked(source)) ? f : 0.0);
        float blockingModifier = blocking.apply((double) damage).floatValue();
        damage += blockingModifier;

        Function<Double, Double> freezing = f -> {
            if (source.is(DamageTypeTags.IS_FREEZING) && this.getType().is(EntityTypeTags.FREEZE_HURTS_EXTRA_TYPES)) {
                return -(f - (f * 5.0F));
            }
            return -0.0;
        };
        float freezingModifier = freezing.apply((double) damage).floatValue();
        damage += freezingModifier;

        Function<Double, Double> hardHat = f -> {
            if (source.is(DamageTypeTags.DAMAGES_HELMET) && !this.getItemBySlot(EquipmentSlot.HEAD).isEmpty()) {
                return -(f - (f * 0.75F));
            }
            return -0.0;
        };
        float hardHatModifier = hardHat.apply((double) damage).floatValue();
        damage += hardHatModifier;

        if ((float) this.invulnerableTime > (float) this.invulnerableDuration / 2.0F && !source.is(DamageTypeTags.BYPASSES_COOLDOWN)) {
            if (damage <= this.lastHurt) {
                if (source.getEntity() instanceof net.minecraft.world.entity.player.Player) {
                    ((net.minecraft.world.entity.player.Player) source.getEntity()).resetAttackStrengthTicker();
                }
                return null;
            }
        }

        Function<Double, Double> armor = f -> {
            if (!source.is(DamageTypeTags.BYPASSES_ARMOR)) {
                return -(f - CombatRules.getDamageAfterAbsorb((LivingEntity) (Object) this, f.floatValue(), source, (float) this.getArmorValue(), (float) this.getAttributeValue(Attributes.ARMOR_TOUGHNESS)));
            }

            return -0.0;
        };
        float armorModifier = armor.apply((double) damage).floatValue();
        damage += armorModifier;

        Function<Double, Double> resistance = f -> {
            if (!source.is(DamageTypeTags.BYPASSES_EFFECTS) && this.hasEffect(MobEffects.DAMAGE_RESISTANCE) && !source.is(DamageTypeTags.BYPASSES_RESISTANCE)) {
                int i = (this.getEffect(MobEffects.DAMAGE_RESISTANCE).getAmplifier() + 1) * 5;
                int j = 25 - i;
                float f1 = f.floatValue() * (float) j;
                return -(f - (f1 / 25.0F));
            }
            return -0.0;
        };
        float resistanceModifier = resistance.apply((double) damage).floatValue();
        damage += resistanceModifier;

        Function<Double, Double> magic = f -> {
            float l;
            if (this.level() instanceof ServerLevel serverLevel) {
                l = EnchantmentHelper.getDamageProtection(serverLevel, (LivingEntity) (Object) this, source);
            } else {
                l = 0.0F;
            }

            if (l > 0.0F) {
                return -(f - CombatRules.getDamageAfterMagicAbsorb(f.floatValue(), l));
            }
            return -0.0;
        };
        float magicModifier = magic.apply((double) damage).floatValue();
        damage += magicModifier;

        Function<Double, Double> absorption = f -> -(Math.max(f - Math.max(f - this.getAbsorptionAmount(), 0.0F), 0.0F));
        float absorptionModifier = absorption.apply((double) damage).floatValue();

        return CraftEventFactory.handleLivingEntityDamageEvent((LivingEntity) (Object) this, source, original, freezingModifier, hardHatModifier, blockingModifier, armorModifier, resistanceModifier, magicModifier, absorptionModifier, freezing, hardHat, blocking, armor, resistance, magic, absorption);
    }

    @Decorate(method = "hurt", inject = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;isDamageSourceBlocked(Lnet/minecraft/world/damagesource/DamageSource;)Z"))
    private void arclight$vanilla$preApplyShield(DamageSource source, float original, @Local(allocate = "bukkitEvent") EntityDamageEvent event) throws Throwable {
        // Special handle; assuming shield cut out at most BLOCKING damage from original and g means the damage that will be used to calculate exact blocking.
        // Merged Bukkit & Modded calculation result with the above assumption. Be careful.
        double blocking = event.getDamage(EntityDamageEvent.DamageModifier.BLOCKING);
        if (blocking == 0.0F) return;
        original = -(float) blocking;
        DecorationOps.blackhole().invoke(original);
    }

    @Decorate(method = "hurt", inject = true, at = @At(value = "INVOKE", ordinal = 2, target = "Lnet/minecraft/world/damagesource/DamageSource;is(Lnet/minecraft/tags/TagKey;)Z"))
    private void arclight$vanilla$postApplyShield(DamageSource source, float original, @Local(allocate = "bukkitEvent") EntityDamageEvent event) throws Throwable {
        double before = -event.getDamage(EntityDamageEvent.DamageModifier.BLOCKING);
        if (before == -0.0F) return;
        double actualOffset = before - original;
        event.setDamage(EntityDamageEvent.DamageModifier.BLOCKING, actualOffset);
    }

    @Unique
    protected float arclight$vanilla$calculateStage(EntityDamageEvent.DamageModifier stage, float original, EntityDamageEvent event) {
        final EntityDamageEventBridge bridge = (EntityDamageEventBridge) event;
        double before = ((EntityDamageEventBridge)event).arclight$accumulateBefore(stage);
        if (!bridge.arclight$applicable(stage)) {
            // Definitely not overridden
            ArclightServer.LOGGER.info("Damage in stage {} use Modded(bypass) offset {}", stage, 0.0F);
            return original;
        }
        double actualOffset = original - before;
        if (bridge.arclight$isStillOriginal(stage, actualOffset)) {
            // If the offset fits vanilla result
            // Override damage using Bukkit value
            actualOffset = event.getDamage(stage);
            ArclightServer.LOGGER.info("Damage in stage {} use Bukkit offset {}", stage, actualOffset);
            return (float) (before + actualOffset);
        } else {
            // Or else, we have to use Modded result
            event.setDamage(stage, actualOffset);
            ArclightServer.LOGGER.info("Damage in stage {} use Modded offset {}", stage, actualOffset);
            return original;
        }
    }

    @Decorate(method = "hurt", inject = true, at = @At(value = "INVOKE", ordinal = 3, target = "Lnet/minecraft/world/damagesource/DamageSource;is(Lnet/minecraft/tags/TagKey;)Z"))
    private void arclight$vanilla$postApplyFreezing(DamageSource source, float original, @Local(allocate = "bukkitEvent") EntityDamageEvent event) throws Throwable {
        original = arclight$vanilla$calculateStage(EntityDamageEvent.DamageModifier.FREEZING, original, event);
        DecorationOps.blackhole().invoke(original);
    }

    @Decorate(method = "hurt", inject = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/WalkAnimationState;setSpeed(F)V"))
    private void arclight$vanilla$postApplyHardHat(DamageSource source, float original, @Local(allocate = "bukkitEvent") EntityDamageEvent event) throws Throwable {
        original = arclight$vanilla$calculateStage(EntityDamageEvent.DamageModifier.HARD_HAT, original, event);
        DecorationOps.blackhole().invoke(original);
    }

    @Decorate(method = "hurt", inject = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;actuallyHurt(Lnet/minecraft/world/damagesource/DamageSource;F)V"))
    private void arclight$vanilla$setCurrentEvent(DamageSource source, float original, @Local(allocate = "bukkitEvent") EntityDamageEvent event) throws Throwable {
        arclight$currentDamageEvent = event;
    }

    @Decorate(method = "actuallyHurt", inject = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getDamageAfterMagicAbsorb(Lnet/minecraft/world/damagesource/DamageSource;F)F"))
    private void arclight$vanilla$postApplyArmor(DamageSource source, float original) throws Throwable {
        original = arclight$vanilla$calculateStage(EntityDamageEvent.DamageModifier.ARMOR, original, arclight$currentDamageEvent);
        DecorationOps.blackhole().invoke(original);
    }

    @Decorate(method = "getDamageAfterMagicAbsorb", at = @At(value = "INVOKE", target = "Ljava/lang/Math;max(FF)F"))
    private float arclight$vanilla$postApplyResistance(float first, float second) throws Throwable {
        float result = (float) DecorationOps.callsite().invoke(first, second);
        result = arclight$vanilla$calculateStage(EntityDamageEvent.DamageModifier.RESISTANCE, result, arclight$currentDamageEvent);
        return result;
    }

    @Decorate(method = "actuallyHurt", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getDamageAfterMagicAbsorb(Lnet/minecraft/world/damagesource/DamageSource;F)F"))
    private float arclight$vanilla$postApplyMagic(LivingEntity entity, DamageSource source, float original) throws Throwable {
        float result = (float) DecorationOps.callsite().invoke(entity, source, original);
        return arclight$vanilla$calculateStage(EntityDamageEvent.DamageModifier.MAGIC, result, arclight$currentDamageEvent);
    }

    @Decorate(method = "actuallyHurt", at = @At(value = "INVOKE", target = "Ljava/lang/Math;max(FF)F"))
    private float arclight$vanilla$postApplyAbsorption(float first, float second, DamageSource source, float original) throws Throwable {
        float result = (float) DecorationOps.callsite().invoke(first, second);
        result = arclight$vanilla$calculateStage(EntityDamageEvent.DamageModifier.ABSORPTION, result, arclight$currentDamageEvent);
        return Math.max(result, 0.0F);
    }
}
