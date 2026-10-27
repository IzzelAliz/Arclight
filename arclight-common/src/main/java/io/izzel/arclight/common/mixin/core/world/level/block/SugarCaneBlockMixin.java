package io.izzel.arclight.common.mixin.core.world.level.block;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;
import io.izzel.arclight.common.bridge.core.world.level.WorldBridge;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.SugarCaneBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SugarCaneBlock.class)
public class SugarCaneBlockMixin {
    @ModifyExpressionValue(method = "randomTick(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/util/RandomSource;)V", at = @At(value="CONSTANT", args="intValue=15"), require = 1, expect = 1, allow = 1)
    private int arclight$growthDecision(int original, BlockState state, ServerLevel level, BlockPos pos, RandomSource random,
                                           @Local(index = 6) int age, @Share("caneModifier") LocalIntRef modifier) {
        int value = ((WorldBridge) level).bridge$spigotConfig().caneModifier;
        modifier.set(value);
        boolean grow = age >= original || (value != 100 && random.nextFloat() < value / 1600.0F);
        // Only lower the branch comparand: the AGE read/local/state remain untouched.
        return grow ? age : (age ^ 1);
    }

    @Inject(method = "randomTick(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/util/RandomSource;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;setValue(Lnet/minecraft/world/level/block/state/properties/Property;Ljava/lang/Comparable;)Ljava/lang/Object;", ordinal = 1), cancellable = true, require = 1, expect = 1, allow = 1)
    private void arclight$incrementDecision(BlockState state, ServerLevel level, BlockPos pos, RandomSource random, CallbackInfo ci,
                                             @Share("caneModifier") LocalIntRef modifier) {
        int value = modifier.get();
        if (value != 100 && !(random.nextFloat() < value / 1600.0F)) ci.cancel();
    }

    // Preserve the pre-existing event bridge; no new upper-call ownership policy.
    @Redirect(method = "randomTick(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/util/RandomSource;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;setBlockAndUpdate(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Z"))
    public boolean arclight$cropGrow(ServerLevel world, BlockPos pos, BlockState state) {
        return CraftEventFactory.handleBlockGrowEvent(world, pos, state, Block.UPDATE_ALL);
    }
}
