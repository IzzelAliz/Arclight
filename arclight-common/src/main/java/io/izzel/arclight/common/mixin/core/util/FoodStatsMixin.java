package io.izzel.arclight.common.mixin.core.util;

import io.izzel.arclight.common.bridge.entity.LivingEntityBridge;
import io.izzel.arclight.common.bridge.entity.player.ServerPlayerEntityBridge;
import io.izzel.arclight.common.bridge.util.FoodStatsBridge;
import net.minecraft.network.protocol.game.ClientboundSetHealthPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FoodData.class)
public abstract class FoodStatsMixin implements FoodStatsBridge {

    // @formatter:off
    @Shadow public int foodLevel;
    @Shadow public abstract void eat(int foodLevelIn, float foodSaturationModifier);
    @Shadow public float saturationLevel;
    // @formatter:on

    private Player entityhuman;

    public void arclight$constructor() {
        throw new RuntimeException();
    }

    public void arclight$constructor(Player playerEntity) {
        arclight$constructor();
        this.entityhuman = playerEntity;
    }

    @Redirect(method = "eat", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/food/FoodData;eat(IF)V"))
    public void arclight$foodLevelChange(FoodData foodStats, int foodLevelIn, float foodSaturationModifier, Item maybeFood, ItemStack stack) {
        FoodProperties food = maybeFood.getFoodProperties();
        int oldFoodLevel = this.foodLevel;
        FoodLevelChangeEvent event = CraftEventFactory.callFoodLevelChangeEvent(entityhuman, food.getNutrition() + oldFoodLevel, stack);
        if (!event.isCancelled()) {
            this.eat(event.getFoodLevel() - oldFoodLevel, food.getSaturationModifier());
        }
        ((ServerPlayerEntityBridge) entityhuman).bridge$getBukkitEntity().sendHealthUpdate();
    }

    @Inject(method = "tick", at = @At(value = "INVOKE_ASSIGN", remap = false, target = "Ljava/lang/Math;max(II)I"))
    public void arclight$foodLevelChange2(Player player, CallbackInfo ci) {
        FoodLevelChangeEvent event = CraftEventFactory.callFoodLevelChangeEvent(entityhuman, Math.max(this.foodLevel - 1, 0));

        if (!event.isCancelled()) {
            this.foodLevel = event.getFoodLevel();
        }

        ((ServerPlayer) entityhuman).connection.send(new ClientboundSetHealthPacket(((ServerPlayerEntityBridge) entityhuman).bridge$getBukkitEntity().getScaledHealth(), this.foodLevel, this.saturationLevel));
    }

    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;heal(F)V"))
    public void arclight$heal(Player player, CallbackInfo ci) {
        ((LivingEntityBridge) player).bridge$pushHealReason(EntityRegainHealthEvent.RegainReason.SATIATED);
    }

    @Override
    public void bridge$setEntityHuman(Player playerEntity) {
        this.entityhuman = playerEntity;
    }
}
