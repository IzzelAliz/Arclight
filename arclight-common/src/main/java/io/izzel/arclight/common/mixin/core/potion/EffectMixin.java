package io.izzel.arclight.common.mixin.core.potion;

import io.izzel.arclight.common.bridge.entity.LivingEntityBridge;
import io.izzel.arclight.common.bridge.entity.player.ServerPlayerEntityBridge;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.play.server.SUpdateHealthPacket;
import net.minecraft.potion.Effect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.FoodStats;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Effect.class)
public class EffectMixin {

    @Inject(method = "performEffect", at = @At(value = "INVOKE", ordinal = 0, target = "Lnet/minecraft/entity/LivingEntity;heal(F)V"))
    public void arclight$healReason1(LivingEntity livingEntity, int amplifier, CallbackInfo ci) {
        ((LivingEntityBridge) livingEntity).bridge$pushHealReason(EntityRegainHealthEvent.RegainReason.MAGIC_REGEN);
    }

    @Inject(method = "performEffect", at = @At(value = "INVOKE", ordinal = 1, target = "Lnet/minecraft/entity/LivingEntity;heal(F)V"))
    public void arclight$healReason2(LivingEntity livingEntity, int amplifier, CallbackInfo ci) {
        ((LivingEntityBridge) livingEntity).bridge$pushHealReason(EntityRegainHealthEvent.RegainReason.MAGIC);
    }

    @Inject(method = "affectEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;heal(F)V"))
    public void arclight$healReason3(Entity source, Entity indirectSource, LivingEntity livingEntity, int amplifier, double health, CallbackInfo ci) {
        ((LivingEntityBridge) livingEntity).bridge$pushHealReason(EntityRegainHealthEvent.RegainReason.MAGIC);
    }

    @Redirect(method = "performEffect", at = @At(value = "FIELD", ordinal = 0, target = "Lnet/minecraft/util/DamageSource;MAGIC:Lnet/minecraft/util/DamageSource;"))
    private DamageSource arclight$redirectPoison() {
        return CraftEventFactory.POISON;
    }

    @Redirect(method = "performEffect", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/FoodStats;addStats(IF)V"))
    public void arclight$foodLevelChange(FoodStats foodStats, int foodLevelIn, float foodSaturationModifier, LivingEntity livingEntity, int amplifier) {
        PlayerEntity playerEntity = ((PlayerEntity) livingEntity);
        int oldFoodLevel = playerEntity.getFoodStats().getFoodLevel();
        FoodLevelChangeEvent event = CraftEventFactory.callFoodLevelChangeEvent(playerEntity, foodLevelIn + oldFoodLevel);
        if (!event.isCancelled()) {
            playerEntity.getFoodStats().addStats(event.getFoodLevel() - oldFoodLevel, foodSaturationModifier);
        }
        ((ServerPlayerEntity) playerEntity).connection.sendPacket(new SUpdateHealthPacket(((ServerPlayerEntityBridge) playerEntity).bridge$getBukkitEntity().getScaledHealth(),
            playerEntity.getFoodStats().getFoodLevel(), playerEntity.getFoodStats().getSaturationLevel()));

    }
}
