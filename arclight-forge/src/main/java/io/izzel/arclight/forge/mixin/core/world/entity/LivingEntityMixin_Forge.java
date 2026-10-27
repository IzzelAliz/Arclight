package io.izzel.arclight.forge.mixin.core.world.entity;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import io.izzel.arclight.common.bridge.core.world.entity.LivingEntityBridge;
import io.izzel.arclight.common.bridge.core.server.level.ServerPlayerBridge;
import io.izzel.arclight.common.mod.util.ArclightCaptures;
import io.izzel.arclight.common.mod.util.ArclightDamageContainer;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import io.izzel.arclight.mixin.Local;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.event.entity.living.ShieldBlockEvent;
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
import java.util.function.Consumer;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin_Forge extends EntityMixin_Forge implements LivingEntityBridge {

    // @formatter:off
    @Shadow protected abstract void dropExperience(ServerLevel level, @Nullable Entity entity);
    // @formatter:on

    @Unique private transient ArclightDamageContainer arclight$shieldDamageContainer;
    @Unique private transient boolean arclight$droppingExperience;

    @Redirect(method = "dropAllDeathLoot(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;)V", at = @At(value = "INVOKE", ordinal = 0, remap = false, target = "Lnet/minecraft/world/entity/LivingEntity;captureDrops(Ljava/util/Collection;)Ljava/util/Collection;"))
    private Collection<ItemEntity> arclight$captureIfNeed(LivingEntity livingEntity, Collection<ItemEntity> value) {
        Collection<ItemEntity> drops = livingEntity.captureDrops();
        return drops == null ? livingEntity.captureDrops(value) : drops;
    }

    @Redirect(method = "dropAllDeathLoot(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;)V", at = @At(value = "INVOKE", remap = false, target = "Ljava/util/Collection;forEach(Ljava/util/function/Consumer;)V"))
    private void arclight$cancelEvent(Collection<ItemEntity> collection, Consumer<ItemEntity> action) {
        if (this instanceof ServerPlayerBridge) {
            // Recapture for ServerPlayerMixin#onDeath.
            this.captureDrops(collection);
        } else {
            collection.forEach(action);
        }
    }

    @Redirect(method = "dropAllDeathLoot(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;dropExperience(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/Entity;)V"))
    private void arclight$dropLater(LivingEntity instance, ServerLevel level, Entity entity) {
    }

    @Inject(method = "dropAllDeathLoot(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;)V", at = @At("RETURN"))
    private void arclight$dropLast(ServerLevel level, DamageSource damageSource, CallbackInfo ci) {
        this.dropExperience(level, damageSource.getEntity());
    }

    @Decorate(method = "hurtServer(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;F)Z", inject = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;isSleeping()Z"))
    private void arclight$entityDamageEvent(ServerLevel level, DamageSource source, float original, @Local(allocate = "arclightDamageContainer") ArclightDamageContainer container) throws Throwable {
        EntityDamageEvent event = arclight$fireEntityDamageEvent(source, original);
        if (event == null || event.isCancelled()) {
            DecorationOps.cancel().invoke(false);
            return;
        }

        original = (float) event.getDamage();
        container = new ArclightDamageContainer(event);
        DecorationOps.blackhole().invoke(original, container);
        // Forge resets the attack ticker in Player.itemAttackInteraction, after its attack hooks.
    }

    @Decorate(method = "hurtServer(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;F)Z", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;applyItemBlocking(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;F)F"))
    private float arclight$forge$applyShield(LivingEntity entity, ServerLevel level, DamageSource source, float damage, @Local(allocate = "arclightDamageContainer") ArclightDamageContainer container) throws Throwable {
        // ShieldBlockEvent now runs in applyItemBlocking, not hurtServer. Keep the
        // context scoped to this entity/call, including cancellation and exceptions.
        ArclightDamageContainer previous = this.arclight$shieldDamageContainer;
        this.arclight$shieldDamageContainer = container;
        try {
            float blocked = (float) DecorationOps.callsite().invoke(entity, level, source, damage);
            container.applyOffset(-blocked);
            return blocked;
        } finally {
            this.arclight$shieldDamageContainer = previous;
        }
    }

    @Decorate(method = "applyItemBlocking(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;F)F", at = @At(value = "INVOKE", remap = false, target = "Lnet/minecraftforge/event/ForgeEventFactory;onShieldBlock(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/damagesource/DamageSource;FLnet/minecraft/world/item/ItemStack;)Lnet/minecraftforge/event/entity/living/ShieldBlockEvent;"))
    private ShieldBlockEvent arclight$forge$postApplyShield(LivingEntity blocker, DamageSource source, float originalBlocked, ItemStack blockedWith) throws Throwable {
        ArclightDamageContainer container = this.arclight$shieldDamageContainer;
        // The pending context belongs to this helper invocation, not direct calls
        // to applyItemBlocking made reentrantly by a Forge shield listener.
        this.arclight$shieldDamageContainer = null;
        ShieldBlockEvent result;
        try {
            result = (ShieldBlockEvent) DecorationOps.callsite().invoke(blocker, source, originalBlocked, blockedWith);
        } finally {
            this.arclight$shieldDamageContainer = container;
        }
        // A cancelled Forge shield event returns null; do not undo its veto.
        // Direct calls to the public blocking helper have no Bukkit damage context.
        if (result != null && container != null && result.getBlockedDamage() == result.getOriginalBlockedDamage()) {
            float bukkit = -(float) container.getBukkit().getDamage(EntityDamageEvent.DamageModifier.BLOCKING);
            // Keep Forge's clamping and shieldTakesDamage decision, including zero blocking.
            result.setBlockedDamage(bukkit);
        }
        return result;
    }

    @Decorate(method = "hurtServer(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;F)Z", inject = true, at = @At(value = "INVOKE", ordinal = 2, target = "Lnet/minecraft/world/damagesource/DamageSource;is(Lnet/minecraft/tags/TagKey;)Z"))
    private void arclight$vanilla$postApplyFreezing(ServerLevel level, DamageSource source, float original, @Local(allocate = "arclightDamageContainer") ArclightDamageContainer container) throws Throwable {
        // IS_FIRE, IS_FREEZING, then DAMAGES_HELMET: freezing is settled here.
        original = container.calculateStage(EntityDamageEvent.DamageModifier.FREEZING, original);
        DecorationOps.blackhole().invoke(original);
    }

    @Decorate(method = "hurtServer(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;F)Z", inject = true, at = @At(value = "INVOKE", target = "Ljava/lang/Float;isNaN(F)Z"))
    private void arclight$vanilla$postApplyHardHat(ServerLevel level, DamageSource source, float original, @Local(allocate = "arclightDamageContainer") ArclightDamageContainer container) throws Throwable {
        original = container.calculateStage(EntityDamageEvent.DamageModifier.HARD_HAT, original);
        DecorationOps.blackhole().invoke(original);
    }

    @Decorate(method = "hurtServer(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;F)Z", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;actuallyHurt(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;F)V"))
    private void arclight$forge$applyDamage(LivingEntity entity, ServerLevel level, DamageSource source, float damage, @Local(allocate = "arclightDamageContainer") ArclightDamageContainer container) throws Throwable {
        // Both cooldown branches dispatch virtually to LivingEntity/Player.actuallyHurt.
        // Own the capture here, so either implementation and all of its exits share
        // one balanced scope without consuming an enclosing damage invocation.
        ArclightCaptures.captureDamageContainer(container);
        try {
            DecorationOps.callsite().invoke(entity, level, source, damage);
        } finally {
            ArclightCaptures.popDamageContainer();
        }
    }

    @Decorate(method = "actuallyHurt(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;F)V", inject = true, at = @At("HEAD"))
    private void arclight$vanilla$getEntityDamageEvent(ServerLevel level, DamageSource source, float damage, @Local(allocate = "arclightDamageContainer") ArclightDamageContainer container) throws Throwable {
        container = ArclightCaptures.getDamageContainer();
        DecorationOps.blackhole().invoke(container);
    }

    @Decorate(method = "actuallyHurt(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;F)V", at = @At(value = "INVOKE", remap = false, target = "Lnet/minecraftforge/common/ForgeHooks;onLivingHurt(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/damagesource/DamageSource;F)F"))
    private float arclight$forge$applyFromLivingHurt(LivingEntity entity, DamageSource source, float amount, @Local(allocate = "arclightDamageContainer") ArclightDamageContainer container) throws Throwable {
        float result = (float) DecorationOps.callsite().invoke(entity, source, amount);
        container.setCurrentDamage(result);
        return result;
    }

    @Decorate(method = "actuallyHurt(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;F)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getDamageAfterArmorAbsorb(Lnet/minecraft/world/damagesource/DamageSource;F)F"))
    private float arclight$vanilla$postApplyArmor(LivingEntity entity, DamageSource source, float damage, @Local(allocate = "arclightDamageContainer") ArclightDamageContainer container) throws Throwable {
        float result = (float) DecorationOps.callsite().invoke(entity, source, damage);
        return container.calculateStage(EntityDamageEvent.DamageModifier.ARMOR, result);
    }

    @Decorate(method = "getDamageAfterMagicAbsorb(Lnet/minecraft/world/damagesource/DamageSource;F)F", at = @At(value = "INVOKE", target = "Ljava/lang/Math;max(FF)F"))
    private float arclight$vanilla$postApplyResistance(float first, float second) throws Throwable {
        float result = (float) DecorationOps.callsite().invoke(first, second);
        return ArclightCaptures.getDamageContainer().calculateStage(EntityDamageEvent.DamageModifier.RESISTANCE, result);
    }

    @Decorate(method = "actuallyHurt(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;F)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getDamageAfterMagicAbsorb(Lnet/minecraft/world/damagesource/DamageSource;F)F"))
    private float arclight$vanilla$postApplyMagic(LivingEntity entity, DamageSource source, float damage, @Local(allocate = "arclightDamageContainer") ArclightDamageContainer container) throws Throwable {
        float result = (float) DecorationOps.callsite().invoke(entity, source, damage);
        return container.calculateStage(EntityDamageEvent.DamageModifier.MAGIC, result);
    }

    @Decorate(method = "actuallyHurt(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;F)V", at = @At(value = "INVOKE", target = "Ljava/lang/Math;max(FF)F"))
    private float arclight$vanilla$postApplyAbsorption(float first, float second, @Local(allocate = "arclightDamageContainer") ArclightDamageContainer container) throws Throwable {
        float result = (float) DecorationOps.callsite().invoke(first, second);
        return Math.max(container.calculateStage(EntityDamageEvent.DamageModifier.ABSORPTION, result), 0.0F);
    }

    @WrapMethod(method = "dropExperience(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/Entity;)V")
    private void arclight$forge$experienceScope(ServerLevel level, Entity attacker, Operation<Void> original) {
        boolean previous = this.arclight$droppingExperience;
        this.arclight$droppingExperience = true;
        try {
            original.call(level, attacker);
        } finally {
            this.arclight$droppingExperience = previous;
        }
    }

    @Override
    public int bridge$forge$getExperienceDrop(LivingEntity entity, Player attackingPlayer, int originalExperience) {
        // Common calls this bridge both while proposing Bukkit XP and while
        // awarding it. Forge's native event belongs only to the physical drop.
        if (!this.arclight$droppingExperience) return originalExperience;
        // A listener's reward query is not another physical drop. A genuinely
        // nested dropExperience invocation opens its own scope above.
        this.arclight$droppingExperience = false;
        try {
            return ForgeEventFactory.getExperienceDrop(entity, attackingPlayer, originalExperience);
        } finally {
            this.arclight$droppingExperience = true;
        }
    }

    @Override
    public boolean bridge$forge$onLivingUseTotem(LivingEntity entity, DamageSource damageSource, ItemStack totem, InteractionHand hand) {
        return ForgeHooks.onLivingUseTotem(entity, damageSource, totem, hand);
    }

    @Override
    public void bridge$forge$onLivingConvert(LivingEntity entity, LivingEntity outcome) {
        ForgeEventFactory.onLivingConvert(entity, outcome);
    }

    @Override
    public boolean bridge$forge$canEntityDestroy(Level level, BlockPos pos, LivingEntity entity) {
        return ForgeHooks.canEntityDestroy((ServerLevel) level, pos, entity);
    }

    @Inject(method = "dropFromShearingLootTable", at = @At("HEAD"), cancellable = true)
    private void arclight$reuseShearingLoot(ServerLevel level,
            net.minecraft.resources.ResourceKey<net.minecraft.world.level.storage.loot.LootTable> table,
            net.minecraft.world.item.ItemInstance tool,
            java.util.function.BiConsumer<ServerLevel, ItemStack> dropper, CallbackInfo ci) {
        var selection = ArclightCaptures.forgeShearDrops;
        if (selection != null && selection.getLeft() == (Object) this && selection.getMiddle() == tool
            && selection.getRight().getFirst().equals(table)) {
            ArclightCaptures.forgeShearDrops = null;
            for (ItemStack drop : selection.getRight().getSecond()) {
                dropper.accept(level, drop.copy());
            }
            ci.cancel();
        }
    }
}
