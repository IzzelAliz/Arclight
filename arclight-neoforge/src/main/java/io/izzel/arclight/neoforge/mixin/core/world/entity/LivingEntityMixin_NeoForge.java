package io.izzel.arclight.neoforge.mixin.core.world.entity;

import io.izzel.arclight.common.bridge.core.entity.LivingEntityBridge;
import io.izzel.arclight.common.bridge.core.entity.player.ServerPlayerEntityBridge;
import io.izzel.arclight.common.mod.server.event.ArclightEventFactory;
import io.izzel.arclight.common.mod.util.NeoForgeDamageModifier;
import io.izzel.arclight.i18n.ArclightConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import net.neoforged.neoforge.event.EventHooks;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.bukkit.event.entity.EntityDamageEvent;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin_NeoForge extends EntityMixin_NeoForge implements LivingEntityBridge {

    // @formatter:off
    @Shadow public abstract boolean isSleeping();
    @Shadow public abstract Collection<MobEffectInstance> getActiveEffects();
    @Shadow protected abstract void dropExperience(@Nullable Entity entity);
    @Shadow public abstract boolean hasEffect(Holder<MobEffect> effect);
    @Shadow public abstract @Nullable MobEffectInstance getEffect(Holder<MobEffect> effect);
    // @formatter:on

    private List<NeoForgeDamageModifier> forgeModifiers = new ArrayList<>();

    @Inject(method = "<init>", at = @At("RETURN"))
    private void arclight$init(CallbackInfo ci) {
        // Add NeoForge damage modifiers
        forgeModifiers.add(new NeoForgeDamageModifier(
            damage -> {
                // Apply NeoForge's damage resistance
                if (this.hasEffect(MobEffects.DAMAGE_RESISTANCE)) {
                    MobEffectInstance effect = this.getEffect(MobEffects.DAMAGE_RESISTANCE);
                    if (effect != null) {
                        int level = effect.getAmplifier();
                        return damage * (1.0f - (level + 1) * 0.2f);
                    }
                }
                return damage;
            },
            "forge_resistance"
        ));
    }

    @Inject(method = "hurt", cancellable = true, at = @At("HEAD"))
    private void arclight$livingHurt(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (!ArclightConfig.spec().getCompat().isUseNeoForgeDamageCalculation()) {
            return;
        }

        // 1. Trigger NeoForge damage event
        DamageContainer container = new DamageContainer(source, amount);
        if (CommonHooks.onEntityIncomingDamage((LivingEntity) (Object) this, container)) {
            cir.setReturnValue(false);
            return;
        }

        // 2. Apply NeoForge damage modifiers
        float modifiedAmount = amount; // Use original amount since DamageContainer doesn't have getAmount()
        for (NeoForgeDamageModifier modifier : forgeModifiers) {
            modifiedAmount = (float) modifier.apply(modifiedAmount);
        }

        // 3. Trigger Bukkit event
        EntityDamageEvent event = CraftEventFactory.handleLivingEntityDamageEvent(
            (LivingEntity) (Object) this,
            source,
            modifiedAmount,
            0.0f, // freezingModifier
            0.0f, // hardHatModifier
            0.0f, // blockingModifier
            0.0f, // armorModifier
            0.0f, // resistanceModifier
            0.0f, // magicModifier
            0.0f, // absorptionModifier
            f -> f, // freezing
            f -> f, // hardHat
            f -> f, // blocking
            f -> f, // armor
            f -> f, // resistance
            f -> f, // magic
            f -> f  // absorption
        );

        // 4. Handle event result
        if (event.isCancelled()) {
            cir.setReturnValue(false);
            return;
        }

        // 5. Update final damage value
        // Since DamageContainer doesn't have setAmount(), we'll just use the event's final damage
        amount = (float) event.getFinalDamage();
    }

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

    @Override
    public List<NeoForgeDamageModifier> bridge$getForgeModifiers() {
        return this.forgeModifiers;
    }
}
