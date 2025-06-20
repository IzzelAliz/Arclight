package io.izzel.arclight.common.mixin.vanilla.world.entity.player;

import io.izzel.arclight.common.mixin.vanilla.world.entity.LivingEntityMixin_Vanilla;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.entity.player.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Player.class)
public abstract class PlayerMixin_Vanilla extends LivingEntityMixin_Vanilla {

    // @formatter:off
    @Shadow public abstract Abilities getAbilities();
    // @formatter:on
    @Decorate(method = "actuallyHurt", inject = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;getDamageAfterMagicAbsorb(Lnet/minecraft/world/damagesource/DamageSource;F)F"))
    private void arclight$vanilla$postApplyArmor(DamageSource source, float original) throws Throwable {
        original = arclight$vanilla$calculateStage(EntityDamageEvent.DamageModifier.ARMOR, original, arclight$currentDamageEvent);
        DecorationOps.blackhole().invoke(original);
    }

    @Decorate(method = "actuallyHurt", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;getDamageAfterMagicAbsorb(Lnet/minecraft/world/damagesource/DamageSource;F)F"))
    private float arclight$vanilla$postApplyMagic(Player player, DamageSource source, float original) throws Throwable {
        float result = (float) DecorationOps.callsite().invoke(player, source, original);
        return arclight$vanilla$calculateStage(EntityDamageEvent.DamageModifier.MAGIC, result, arclight$currentDamageEvent);
    }

    @Decorate(method = "actuallyHurt", at = @At(value = "INVOKE", target = "Ljava/lang/Math;max(FF)F"))
    private float arclight$vanilla$postApplyAbsorption(float first, float second, DamageSource source, float original) throws Throwable {
        float result = (float) DecorationOps.callsite().invoke(first, second);
        result = arclight$vanilla$calculateStage(EntityDamageEvent.DamageModifier.ABSORPTION, result, arclight$currentDamageEvent);
        return Math.max(result, 0.0F);
    }
}
