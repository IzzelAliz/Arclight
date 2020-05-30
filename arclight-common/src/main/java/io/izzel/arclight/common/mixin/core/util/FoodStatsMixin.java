package io.izzel.arclight.common.mixin.core.util;

import io.izzel.arclight.common.bridge.entity.LivingEntityBridge;
import io.izzel.arclight.common.bridge.entity.player.ServerPlayerEntityBridge;
import io.izzel.arclight.common.bridge.util.FoodStatsBridge;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.Food;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.server.SUpdateHealthPacket;
import net.minecraft.util.FoodStats;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FoodStats.class)
public abstract class FoodStatsMixin implements FoodStatsBridge {

    // @formatter:off
    @Shadow public int foodLevel;
    @Shadow public abstract void addStats(int foodLevelIn, float foodSaturationModifier);
    @Shadow public float foodSaturationLevel;
    // @formatter:on

    private PlayerEntity entityhuman;

    public void arclight$constructor() {
        throw new RuntimeException();
    }

    public void arclight$constructor(PlayerEntity playerEntity) {
        arclight$constructor();
        this.entityhuman = playerEntity;
    }

    @Redirect(method = "consume", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/FoodStats;addStats(IF)V"))
    public void arclight$foodLevelChange(FoodStats foodStats, int foodLevelIn, float foodSaturationModifier, Item maybeFood, ItemStack stack) {
        Food food = maybeFood.getFood();
        int oldFoodLevel = this.foodLevel;
        FoodLevelChangeEvent event = CraftEventFactory.callFoodLevelChangeEvent(entityhuman, food.getHealing() + oldFoodLevel, stack);
        if (!event.isCancelled()) {
            this.addStats(event.getFoodLevel() - oldFoodLevel, food.getSaturation());
        }
        ((ServerPlayerEntityBridge) entityhuman).bridge$getBukkitEntity().sendHealthUpdate();
    }

    @Inject(method = "tick", at = @At(value = "INVOKE_ASSIGN", remap = false, target = "Ljava/lang/Math;max(II)I"))
    public void arclight$foodLevelChange2(PlayerEntity player, CallbackInfo ci) {
        FoodLevelChangeEvent event = CraftEventFactory.callFoodLevelChangeEvent(entityhuman, Math.max(this.foodLevel - 1, 0));

        if (!event.isCancelled()) {
            this.foodLevel = event.getFoodLevel();
        }

        ((ServerPlayerEntity) entityhuman).connection.sendPacket(new SUpdateHealthPacket(((ServerPlayerEntityBridge) entityhuman).bridge$getBukkitEntity().getScaledHealth(), this.foodLevel, this.foodSaturationLevel));
    }

    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerEntity;heal(F)V"))
    public void arclight$heal(PlayerEntity player, CallbackInfo ci) {
        ((LivingEntityBridge) player).bridge$pushHealReason(EntityRegainHealthEvent.RegainReason.SATIATED);
    }

    @Override
    public void bridge$setEntityHuman(PlayerEntity playerEntity) {
        this.entityhuman = playerEntity;
    }
}
