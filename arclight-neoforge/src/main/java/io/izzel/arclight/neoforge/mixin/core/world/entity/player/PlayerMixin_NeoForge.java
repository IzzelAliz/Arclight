package io.izzel.arclight.neoforge.mixin.core.world.entity.player;

import io.izzel.arclight.common.bridge.core.entity.player.PlayerEntityBridge;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import io.izzel.arclight.neoforge.mixin.core.world.entity.LivingEntityMixin_NeoForge;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import net.neoforged.neoforge.common.extensions.IPlayerExtension;
import org.bukkit.event.entity.EntityDamageEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class PlayerMixin_NeoForge extends LivingEntityMixin_NeoForge implements PlayerEntityBridge, IPlayerExtension {

    // @formatter:off
    @Shadow public abstract Abilities getAbilities();
    @Shadow public AbstractContainerMenu containerMenu;
    // @formatter:on

    @Shadow public abstract boolean isCreative();

    @Inject(method = "hurt", cancellable = true, at = @At("HEAD"))
    private void arclight$onPlayerAttack(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (CommonHooks.onEntityIncomingDamage((Player) (Object) this, new DamageContainer(source, amount))) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "attack", cancellable = true, at = @At("HEAD"))
    private void arclight$onPlayerAttackTarget(Entity entity, CallbackInfo ci) {
        if (!CommonHooks.onPlayerAttackTarget((Player) (Object) this, entity)) {
            ci.cancel();
        }
    }

    @Decorate(method = "actuallyHurt", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;getDamageAfterArmorAbsorb(Lnet/minecraft/world/damagesource/DamageSource;F)F"))
    private float arclight$neoforge$postApplyArmor(Player player, DamageSource source, float original) throws Throwable {
        float result = (float) DecorationOps.callsite().invoke(player, source, original);
        result = arclight$neoforge$calculateStage(EntityDamageEvent.DamageModifier.ARMOR, result, arclight$currentDamageEvent);
        return result;
    }

    @Decorate(method = "actuallyHurt", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;getDamageAfterMagicAbsorb(Lnet/minecraft/world/damagesource/DamageSource;F)F"))
    private float arclight$neoforge$postApplyMagic(Player player, DamageSource source, float original) throws Throwable {
        float result = (float) DecorationOps.callsite().invoke(player, source, original);
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
    public boolean bridge$platform$mayfly() {
        return this.mayFly();
    }
}
