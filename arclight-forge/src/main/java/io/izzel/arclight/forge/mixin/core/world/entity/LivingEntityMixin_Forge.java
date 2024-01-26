package io.izzel.arclight.forge.mixin.core.world.entity;

import io.izzel.arclight.common.bridge.core.entity.LivingEntityBridge;
import io.izzel.arclight.common.bridge.core.entity.player.ServerPlayerEntityBridge;
import io.izzel.tools.product.Product;
import io.izzel.tools.product.Product3;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collection;
import java.util.function.Consumer;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin_Forge extends EntityMixin_Forge implements LivingEntityBridge {

    // @formatter:off
    @Shadow public abstract boolean isSleeping();
    @Shadow public abstract Collection<MobEffectInstance> getActiveEffects();
    @Shadow protected abstract void dropExperience();
    // @formatter:on

    @Inject(method = "hurt", cancellable = true, at = @At("HEAD"))
    private void arclight$livingHurt(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (!ForgeHooks.onLivingAttack((LivingEntity) (Object) this, source, amount)) {
            cir.setReturnValue(false);
        }
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

    @Redirect(method = "dropAllDeathLoot", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;dropExperience()V"))
    private void arclight$dropLater(LivingEntity livingEntity) {
    }

    @Inject(method = "dropAllDeathLoot", at = @At("RETURN"))
    private void arclight$dropLast(DamageSource damageSourceIn, CallbackInfo ci) {
        this.dropExperience();
    }

    @Override
    public boolean bridge$forge$mobEffectExpired(MobEffectInstance effect) {
        return MinecraftForge.EVENT_BUS.post(new MobEffectEvent.Expired((LivingEntity) (Object) this, effect));
    }

    @Override
    public boolean bridge$forge$mobEffectAdded(MobEffectInstance old, MobEffectInstance effect, Entity entity) {
        return MinecraftForge.EVENT_BUS.post(new MobEffectEvent.Added((LivingEntity) (Object) this, old, effect, entity));
    }

    @Override
    public float bridge$forge$onLivingHurt(LivingEntity entity, DamageSource src, float amount) {
        return ForgeHooks.onLivingHurt(entity, src, amount);
    }

    @Override
    public Product3<Boolean, Float, Boolean> bridge$forge$onShieldBlock(LivingEntity blocker, DamageSource source, float blocked) {
        var event = ForgeEventFactory.onShieldBlock(blocker, source, blocked);
        return Product.of(event.isCanceled(), event.getBlockedDamage(), event.shieldTakesDamage());
    }

    @Override
    public float bridge$forge$onLivingDamage(LivingEntity entity, DamageSource src, float amount) {
        return ForgeHooks.onLivingDamage(entity, src, amount);
    }

    @Override
    public boolean bridge$forge$onLivingUseTotem(LivingEntity entity, DamageSource damageSource, ItemStack totem, InteractionHand hand) {
        return ForgeHooks.onLivingUseTotem(entity, damageSource, totem, hand);
    }

    @Nullable
    @Override
    public LivingEntity bridge$forge$onLivingChangeTarget(LivingEntity entity, LivingEntity originalTarget, LivingTargetType targetType) {
        var event = ForgeHooks.onLivingChangeTarget(entity, originalTarget, LivingChangeTargetEvent.LivingTargetType.valueOf(targetType.name()));
        return event.isCanceled() ? null : event.getNewTarget();
    }

    @Override
    public BlockPos bridge$forge$onEnderTeleport(LivingEntity entity, double targetX, double targetY, double targetZ) {
        var event = ForgeEventFactory.onEnderTeleport(entity, targetX, targetY, targetZ);
        return event.isCanceled() ? null : BlockPos.containing(event.getTarget());
    }

    @Override
    public void bridge$forge$onLivingConvert(LivingEntity entity, LivingEntity outcome) {
        ForgeEventFactory.onLivingConvert(entity, outcome);
    }

    @Override
    public boolean bridge$forge$canEntityDestroy(Level level, BlockPos pos, LivingEntity entity) {
        return ForgeHooks.canEntityDestroy(level, pos, entity);
    }

    @Override
    public boolean bridge$forge$onEntityDestroyBlock(LivingEntity entity, BlockPos pos, BlockState state) {
        return ForgeEventFactory.onEntityDestroyBlock(entity, pos, state);
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
    public void bridge$common$finishCaptureAndFireEvent() {
    }
}
