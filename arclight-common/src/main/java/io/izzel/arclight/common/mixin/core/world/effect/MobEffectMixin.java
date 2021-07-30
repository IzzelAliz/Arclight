package io.izzel.arclight.common.mixin.core.world.effect;

import io.izzel.arclight.common.bridge.core.entity.LivingEntityBridge;
import io.izzel.arclight.common.bridge.core.entity.player.ServerPlayerEntityBridge;
import net.minecraft.network.protocol.game.ClientboundSetHealthPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MobEffect.class)
public class MobEffectMixin {

    @Inject(method = "applyEffectTick", at = @At(value = "INVOKE", ordinal = 0, target = "Lnet/minecraft/world/entity/LivingEntity;heal(F)V"))
    public void arclight$healReason1(LivingEntity livingEntity, int amplifier, CallbackInfo ci) {
        ((LivingEntityBridge) livingEntity).bridge$pushHealReason(EntityRegainHealthEvent.RegainReason.MAGIC_REGEN);
    }

    @Inject(method = "applyEffectTick", at = @At(value = "INVOKE", ordinal = 1, target = "Lnet/minecraft/world/entity/LivingEntity;heal(F)V"))
    public void arclight$healReason2(LivingEntity livingEntity, int amplifier, CallbackInfo ci) {
        ((LivingEntityBridge) livingEntity).bridge$pushHealReason(EntityRegainHealthEvent.RegainReason.MAGIC);
    }

    @Inject(method = "applyInstantenousEffect", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;heal(F)V"))
    public void arclight$healReason3(Entity source, Entity indirectSource, LivingEntity livingEntity, int amplifier, double health, CallbackInfo ci) {
        ((LivingEntityBridge) livingEntity).bridge$pushHealReason(EntityRegainHealthEvent.RegainReason.MAGIC);
    }

    @Redirect(method = "applyEffectTick", at = @At(value = "FIELD", ordinal = 0, target = "Lnet/minecraft/world/damagesource/DamageSource;MAGIC:Lnet/minecraft/world/damagesource/DamageSource;"))
    private DamageSource arclight$redirectPoison() {
        return CraftEventFactory.POISON;
    }

    @Redirect(method = "applyEffectTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/food/FoodData;eat(IF)V"))
    public void arclight$foodLevelChange(FoodData foodStats, int foodLevelIn, float foodSaturationModifier, LivingEntity livingEntity, int amplifier) {
        Player playerEntity = ((Player) livingEntity);
        int oldFoodLevel = playerEntity.getFoodData().getFoodLevel();
        FoodLevelChangeEvent event = CraftEventFactory.callFoodLevelChangeEvent(playerEntity, foodLevelIn + oldFoodLevel);
        if (!event.isCancelled()) {
            playerEntity.getFoodData().eat(event.getFoodLevel() - oldFoodLevel, foodSaturationModifier);
        }
        ((ServerPlayer) playerEntity).connection.send(new ClientboundSetHealthPacket(((ServerPlayerEntityBridge) playerEntity).bridge$getBukkitEntity().getScaledHealth(),
            playerEntity.getFoodData().getFoodLevel(), playerEntity.getFoodData().getSaturationLevel()));

    }
}
