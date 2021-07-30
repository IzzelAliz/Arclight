package io.izzel.arclight.common.mixin.core.world.level.block;

import io.izzel.arclight.common.bridge.core.entity.player.ServerPlayerEntityBridge;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.CakeBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(CakeBlock.class)
public class CakeBlockMixin {

    @Redirect(method = "eat", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/food/FoodData;eat(IF)V"))
    private static void arclight$eatCake(FoodData foodStats, int foodLevelIn, float foodSaturationModifier, LevelAccessor worldIn, BlockPos pos, BlockState state, Player player) {
        int old = foodStats.getFoodLevel();
        FoodLevelChangeEvent event = CraftEventFactory.callFoodLevelChangeEvent(player, old + foodLevelIn);
        if (!event.isCancelled()) {
            foodStats.eat(event.getFoodLevel() - old, foodSaturationModifier);
        }
        ((ServerPlayerEntityBridge) player).bridge$getBukkitEntity().sendHealthUpdate();
    }
}
