package io.izzel.arclight.forge.mixin.core.world.entity.player;

import io.izzel.arclight.common.bridge.core.world.entity.player.PlayerBridge;
import io.izzel.arclight.common.mod.util.ArclightCaptures;
import io.izzel.arclight.common.mod.util.ArclightDamageContainer;
import io.izzel.arclight.forge.mixin.core.world.entity.LivingEntityMixin_Forge;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import io.izzel.arclight.mixin.Local;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.extensions.IForgePlayer;
import org.bukkit.event.entity.EntityDamageEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class PlayerMixin_Forge extends LivingEntityMixin_Forge implements PlayerBridge, IForgePlayer {

    @Inject(method = "hurtServer(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;F)Z", at = @At("HEAD"), cancellable = true)
    private void arclight$forge$onPlayerAttack(ServerLevel level, DamageSource source, float damage, CallbackInfoReturnable<Boolean> cir) {
        // The common Player.hurtServer overwrite replaces Forge's original entry hook.
        if (!ForgeHooks.onPlayerAttack((Player) (Object) this, source, damage)) {
            cir.setReturnValue(false);
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

    @Decorate(method = "actuallyHurt(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;F)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;getDamageAfterArmorAbsorb(Lnet/minecraft/world/damagesource/DamageSource;F)F"))
    private float arclight$vanilla$postApplyArmor(Player player, DamageSource source, float damage, @Local(allocate = "arclightDamageContainer") ArclightDamageContainer container) throws Throwable {
        float result = (float) DecorationOps.callsite().invoke(player, source, damage);
        return container.calculateStage(EntityDamageEvent.DamageModifier.ARMOR, result);
    }

    @Decorate(method = "actuallyHurt(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;F)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;getDamageAfterMagicAbsorb(Lnet/minecraft/world/damagesource/DamageSource;F)F"))
    private float arclight$vanilla$postApplyMagic(Player player, DamageSource source, float damage, @Local(allocate = "arclightDamageContainer") ArclightDamageContainer container) throws Throwable {
        float result = (float) DecorationOps.callsite().invoke(player, source, damage);
        return container.calculateStage(EntityDamageEvent.DamageModifier.MAGIC, result);
    }

    @Decorate(method = "actuallyHurt(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;F)V", at = @At(value = "INVOKE", target = "Ljava/lang/Math;max(FF)F"))
    private float arclight$vanilla$postApplyAbsorption(float first, float second, @Local(allocate = "arclightDamageContainer") ArclightDamageContainer container) throws Throwable {
        float result = (float) DecorationOps.callsite().invoke(first, second);
        return Math.max(container.calculateStage(EntityDamageEvent.DamageModifier.ABSORPTION, result), 0.0F);
    }

    // LivingEntity.hurtServer owns the capture around the virtual actuallyHurt call;
    // Player must not pop it separately (including onLivingHurt's early return).
}
