package io.izzel.arclight.common.mixin.core.world.food;

import io.izzel.arclight.common.bridge.core.world.entity.LivingEntityBridge;
import io.izzel.arclight.common.bridge.core.world.entity.player.PlayerBridge;
import io.izzel.arclight.common.bridge.core.server.level.ServerPlayerBridge;
import io.izzel.arclight.common.bridge.core.world.food.FoodDataBridge;
import io.izzel.arclight.common.mod.mixins.annotation.CreateConstructor;
import io.izzel.arclight.common.mod.mixins.annotation.ShadowConstructor;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import net.minecraft.network.protocol.game.ClientboundSetHealthPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.bukkit.event.entity.EntityExhaustionEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FoodData.class)
public abstract class FoodDataMixin implements FoodDataBridge {

    // @formatter:off
    @Shadow public int foodLevel;
    @Shadow public abstract void eat(int foodLevelIn, float foodSaturationModifier);
    @Shadow public float saturationLevel;
    // @formatter:on

    private Player entityhuman;
    public int saturatedRegenRate = 10;
    public int unsaturatedRegenRate = 80;
    public int starvationRate = 80;

    @ShadowConstructor
    public void arclight$constructor() {
        throw new RuntimeException();
    }

    @CreateConstructor
    public void arclight$constructor(Player playerEntity) {
        arclight$constructor();
        this.entityhuman = playerEntity;
    }

    private transient ItemStack arclight$eatStack;

    @Override
    public void bridge$pushEatStack(ItemStack stack) {
        this.arclight$eatStack = stack;
    }

    @Decorate(method = "eat(Lnet/minecraft/world/food/FoodProperties;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/food/FoodData;add(IF)V"))
    private void arclight$foodLevelChange(FoodData foodStats, int foodLevelIn, float foodSaturationModifier, FoodProperties food) throws Throwable {
        var stack = this.arclight$eatStack;
        this.arclight$eatStack = null;
        int deltaFoodLevel = foodLevelIn;
        if (this.entityhuman != null && stack != null) {
            int newFoodLevel = Mth.clamp(this.foodLevel + foodLevelIn, 0, 20);
            FoodLevelChangeEvent event = CraftEventFactory.callFoodLevelChangeEvent(this.entityhuman, newFoodLevel, stack);
            if (event.isCancelled()) {
                return;
            }
            deltaFoodLevel = event.getFoodLevel() - this.foodLevel;
            ((ServerPlayerBridge) this.entityhuman).bridge$getBukkitEntity().sendHealthUpdate();
        }
        DecorationOps.callsite().invoke(foodStats, deltaFoodLevel, foodSaturationModifier);
    }

    @Decorate(method = "tick", at = @At(value = "INVOKE", remap = false, target = "Ljava/lang/Math;max(II)I"))
    private int arclight$foodLevelChange2(int candidate, int minimum, ServerPlayer player) throws Throwable {
        int proposed = (int) DecorationOps.callsite().invoke(candidate, minimum);
        if (this.entityhuman == null) this.entityhuman = player;
        FoodLevelChangeEvent event = CraftEventFactory.callFoodLevelChangeEvent(player, proposed);
        int result = event.isCancelled() ? this.foodLevel : event.getFoodLevel();
        player.connection.send(new ClientboundSetHealthPacket(((ServerPlayerBridge) player).bridge$getBukkitEntity().getScaledHealth(), result, this.saturationLevel));
        return result;
    }

    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;heal(F)V"))
    public void arclight$heal(ServerPlayer player, CallbackInfo ci) {
        if (entityhuman == null) {
            entityhuman = player;
        }
        ((LivingEntityBridge) player).bridge$pushHealReason(EntityRegainHealthEvent.RegainReason.SATIATED);
        ((PlayerBridge) player).bridge$pushExhaustReason(EntityExhaustionEvent.ExhaustionReason.REGEN);
    }

    @Override
    public void bridge$setEntityHuman(Player playerEntity) {
        this.entityhuman = playerEntity;
    }

    @Override
    public Player bridge$getEntityHuman() {
        return this.entityhuman;
    }
}
