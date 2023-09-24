package io.izzel.arclight.common.mixin.core.world.effect;

import io.izzel.arclight.common.bridge.core.entity.player.ServerPlayerEntityBridge;
import net.minecraft.network.protocol.game.ClientboundSetHealthPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(targets = "net.minecraft.world.effect.SaturationMobEffect")
public class SaturationMobEffectMixin {

    @Redirect(method = "applyEffectTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/food/FoodData;eat(IF)V"))
    private void arclight$foodLevelChange(FoodData foodStats, int foodLevelIn, float foodSaturationModifier, LivingEntity livingEntity, int amplifier) {
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
