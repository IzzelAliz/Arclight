package io.izzel.arclight.neoforge.mixin.core.world.entity;

import com.google.common.base.Function;
import io.izzel.arclight.common.bridge.bukkit.EntityDamageEventBridge;
import io.izzel.arclight.common.bridge.core.entity.LivingEntityBridge;
import io.izzel.arclight.common.bridge.core.entity.player.ServerPlayerEntityBridge;
import io.izzel.arclight.common.mod.server.ArclightServer;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import io.izzel.arclight.mixin.Local;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.CombatRules;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.event.entity.living.LivingShieldBlockEvent;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.bukkit.event.entity.EntityDamageEvent;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collection;
import java.util.Stack;
import java.util.function.Consumer;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin_NeoForge extends EntityMixin_NeoForge implements LivingEntityBridge {

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
    @Shadow protected abstract void dropExperience(@Nullable Entity entity);
    @Shadow protected Stack<DamageContainer> damageContainers;
    // @formatter:on

    @Shadow public abstract void remove(Entity.RemovalReason arg);

    protected transient EntityDamageEvent arclight$currentDamageEvent;

    @Redirect(method = "dropAllDeathLoot", at = @At(value = "INVOKE", ordinal = 0, remap = false, target = "Lnet/minecraft/world/entity/LivingEntity;captureDrops(Ljava/util/Collection;)Ljava/util/Collection;"))
    private Collection<ItemEntity> arclight$captureIfNeed(LivingEntity
                                                              livingEntity, Collection<ItemEntity> value) {
        Collection<ItemEntity> drops = livingEntity.captureDrops();
        // todo this instanceof ArmorStandEntity
        return drops == null ? livingEntity.captureDrops(value) : drops;
    }

    @Redirect(method = "dropAllDeathLoot", at = @At(value = "INVOKE", remap = false, target = "Ljava/util/Collection;forEach(Ljava/util/function/Consumer;)V"))
    private void arclight$cancelEvent(Collection<ItemEntity> collection, Consumer<ItemEntity> action) {
        if (this instanceof ServerPlayerEntityBridge) {
            // recapture for ServerPlayerEntityMixin#onDeath
            this.captureDrops(collection);
        } else {
            collection.forEach(action);
        }
    }

    @Redirect(method = "dropAllDeathLoot", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;dropExperience(Lnet/minecraft/world/entity/Entity;)V"))
    private void arclight$dropLater(LivingEntity instance, Entity entity) {
    }

    @Inject(method = "dropAllDeathLoot", at = @At("RETURN"))
    private void arclight$dropLast(ServerLevel arg, DamageSource damageSource, CallbackInfo ci) {
        this.dropExperience(damageSource.getEntity());
    }

    @Decorate(method = "hurt", at = @At(value = "INVOKE", ordinal = 0, target = "Lnet/neoforged/neoforge/common/damagesource/DamageContainer;getNewDamage()F"))
    private float arclight$neoforge$entityDamageEvent(DamageContainer instance, DamageSource source, float original, @Local(allocate = "bukkitEvent") EntityDamageEvent event) throws Throwable {
        float result = (float) DecorationOps.callsite().invoke(instance);
        event = arclight$fireEvent(source, result);
        damageContainers.peek().setNewDamage((float) event.getDamage());

        if (event == null || event.isCancelled()) {
            this.damageContainers.pop();
            return (float) DecorationOps.cancel().invoke(false);
        }

        if (source.getEntity() instanceof net.minecraft.world.entity.player.Player player) {
            player.resetAttackStrengthTicker();
        }
        return result;
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

    @Decorate(method = "hurt", at = @At(value = "INVOKE", target = "Lnet/neoforged/neoforge/common/CommonHooks;onDamageBlock(Lnet/minecraft/world/entity/LivingEntity;Lnet/neoforged/neoforge/common/damagesource/DamageContainer;Z)Lnet/neoforged/neoforge/event/entity/living/LivingShieldBlockEvent;"))
    private LivingShieldBlockEvent arclight$neoforge$postApplyShield(LivingEntity blocker, DamageContainer container, boolean originalBlocked, @Local(allocate = "bukkitEvent") EntityDamageEvent event) throws Throwable {
        LivingShieldBlockEvent result = (LivingShieldBlockEvent) DecorationOps.callsite().invoke(blocker, container, originalBlocked);
        float original = result.getOriginalBlockedDamage();
        float bukkit = -(float) event.getDamage(EntityDamageEvent.DamageModifier.BLOCKING);
        ArclightServer.LOGGER.info("Bukkit event damage: {}", bukkit);
        if (originalBlocked == result.getBlocked() && result.getBlockedDamage() == result.getOriginalBlockedDamage()) {
            if (bukkit > 0.0F) {
                result.setBlocked(true);
                result.setBlockedDamage(bukkit);
            } else {
                result.setBlocked(false);
            }
        }
        ArclightServer.LOGGER.info("Shield damage event: blocked {}, blockedDamage {}", result.getBlocked(), result.getBlockedDamage());
        return result;
    }

    @Unique
    protected float arclight$neoforge$calculateStage(EntityDamageEvent.DamageModifier stage, float original, EntityDamageEvent event) {
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
    private void arclight$neoforge$postApplyFreezing(DamageSource source, float original, @Local(allocate = "bukkitEvent") EntityDamageEvent event) throws Throwable {
        original = arclight$neoforge$calculateStage(EntityDamageEvent.DamageModifier.FREEZING, original, event);
        DecorationOps.blackhole().invoke(original);
    }

    @Decorate(method = "hurt", at = @At(value = "INVOKE", target = "Lnet/neoforged/neoforge/common/damagesource/DamageContainer;setNewDamage(F)V"))
    private void arclight$neoforge$postApplyHardHat(DamageContainer container, float arg, @Local(allocate = "bukkitEvent") EntityDamageEvent event) throws Throwable {
        arg = arclight$neoforge$calculateStage(EntityDamageEvent.DamageModifier.HARD_HAT, arg, event);
        DecorationOps.callsite().invoke(container, arg);
    }

    @Decorate(method = "hurt", inject = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;actuallyHurt(Lnet/minecraft/world/damagesource/DamageSource;F)V"))
    private void arclight$neoforge$setCurrentEvent(DamageSource source, float original, @Local(allocate = "bukkitEvent") EntityDamageEvent event) throws Throwable {
        arclight$currentDamageEvent = event;
    }

    @Redirect(method = "hurt", at = @At(value = "INVOKE", ordinal = 2, target = "Lnet/neoforged/neoforge/common/damagesource/DamageContainer;getNewDamage()F"))
    private float arclight$neoforge$debug(DamageContainer instance) {
        float result = instance.getNewDamage();
        ArclightServer.LOGGER.info("Final damage is {}", result);
        return result;
    }

    @Decorate(method = "actuallyHurt", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getDamageAfterArmorAbsorb(Lnet/minecraft/world/damagesource/DamageSource;F)F"))
    private float arclight$neoforge$postApplyArmor(LivingEntity entity, DamageSource source, float original) throws Throwable {
        float result = (float) DecorationOps.callsite().invoke(entity, source, original);
        result = arclight$neoforge$calculateStage(EntityDamageEvent.DamageModifier.ARMOR, result, arclight$currentDamageEvent);
        return result;
    }

    @Decorate(method = "getDamageAfterMagicAbsorb", at = @At(value = "INVOKE", target = "Ljava/lang/Math;max(FF)F"))
    private float arclight$neoforge$postApplyResistance(float first, float second) throws Throwable {
        float result = (float) DecorationOps.callsite().invoke(first, second);
        result = arclight$neoforge$calculateStage(EntityDamageEvent.DamageModifier.RESISTANCE, result, arclight$currentDamageEvent);
        return result;
    }

    @Decorate(method = "actuallyHurt", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getDamageAfterMagicAbsorb(Lnet/minecraft/world/damagesource/DamageSource;F)F"))
    private float arclight$neoforge$postApplyMagic(LivingEntity entity, DamageSource source, float original) throws Throwable {
        float result = (float) DecorationOps.callsite().invoke(entity, source, original);
        float newResult = arclight$neoforge$calculateStage(EntityDamageEvent.DamageModifier.MAGIC, result, arclight$currentDamageEvent);
        if (Math.abs(result - newResult) > 10E-3) {
            DamageContainer container = damageContainers.peek();
            container.setNewDamage(original);
            container.setReduction(DamageContainer.Reduction.ENCHANTMENTS, original - newResult);
        }
        return newResult;
    }

    @Decorate(method = "actuallyHurt", at = @At(value = "INVOKE", ordinal = 1, target = "Lnet/neoforged/neoforge/common/damagesource/DamageContainer;setReduction(Lnet/neoforged/neoforge/common/damagesource/DamageContainer$Reduction;F)V"))
    private void arclight$vanilla$postApplyAbsorption(DamageContainer container, DamageContainer.Reduction reduction, float amount, DamageSource source, float original) throws Throwable {
        float currentDamage = damageContainers.peek().getNewDamage();
        float exactDamage = currentDamage - amount;
        float afterDamage = arclight$neoforge$calculateStage(EntityDamageEvent.DamageModifier.ABSORPTION, exactDamage, arclight$currentDamageEvent);
        if (Math.abs(afterDamage - exactDamage) > 10E-3) {
            amount = currentDamage - afterDamage;
        }
        DecorationOps.callsite().invoke(container, reduction, amount);
    }

    @Override
    public boolean bridge$forge$onLivingUseTotem(LivingEntity entity, DamageSource damageSource, ItemStack totem, InteractionHand hand) {
        return CommonHooks.onLivingUseTotem(entity, damageSource, totem, hand);
    }

    @Override
    public void bridge$forge$onLivingConvert(LivingEntity entity, LivingEntity outcome) {
        EventHooks.onLivingConvert(entity, outcome);
    }

    @Override
    public boolean bridge$forge$canEntityDestroy(Level level, BlockPos pos, LivingEntity entity) {
        return CommonHooks.canEntityDestroy(level, pos, entity);
    }

    @Override
    public void bridge$common$startCaptureDrops() {
    }

    @Override
    public boolean bridge$common$isCapturingDrops() {
        return false;
    }

    @Override
    public void bridge$common$captureDrop(ItemEntity itemEntity) {
    }

    @Override
    public Collection<ItemEntity> bridge$common$getCapturedDrops() {
        return this.captureDrops(null);
    }

    @Override
    public void bridge$common$finishCaptureAndFireEvent(DamageSource damageSource) {
    }
}
