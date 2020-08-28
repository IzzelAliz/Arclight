package io.izzel.arclight.common.mixin.core.block;

import io.izzel.arclight.common.bridge.entity.player.ServerPlayerEntityBridge;
import net.minecraft.block.BlockState;
import net.minecraft.block.CakeBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.FoodStats;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IWorld;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(CakeBlock.class)
public class CakeBlockMixin {

    @Redirect(method = "eatSlice", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/FoodStats;addStats(IF)V"))
    private void arclight$eatCake(FoodStats foodStats, int foodLevelIn, float foodSaturationModifier, IWorld worldIn, BlockPos pos, BlockState state, PlayerEntity player) {
        int old = foodStats.getFoodLevel();
        FoodLevelChangeEvent event = CraftEventFactory.callFoodLevelChangeEvent(player, old + foodLevelIn);
        if (!event.isCancelled()) {
            foodStats.addStats(event.getFoodLevel() - old, foodSaturationModifier);
        }
        ((ServerPlayerEntityBridge) player).bridge$getBukkitEntity().sendHealthUpdate();
    }
}
