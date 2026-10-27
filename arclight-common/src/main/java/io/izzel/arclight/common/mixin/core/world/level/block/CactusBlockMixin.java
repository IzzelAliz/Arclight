package io.izzel.arclight.common.mixin.core.world.level.block;

import io.izzel.arclight.common.bridge.core.world.damagesource.DamageSourceBridge;
import io.izzel.arclight.common.bridge.core.world.level.WorldBridge;
import io.izzel.arclight.mixin.Eject;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageSources;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CactusBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.craftbukkit.block.CraftBlock;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CactusBlock.class)
public class CactusBlockMixin {

    @Redirect(method = "entityInside", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/damagesource/DamageSources;cactus()Lnet/minecraft/world/damagesource/DamageSource;"))
    private DamageSource arclight$cactusDamage1(DamageSources instance, BlockState blockState, Level level, BlockPos blockPos) {
        return ((DamageSourceBridge) instance.cactus()).bridge$directBlock(CraftBlock.at(level, blockPos));
    }

    // The age-8 comparison is after height checks and Neo canCropGrow,
    // but before flower / ordinary cactus / age increment and Neo post.
    @Inject(method = "randomTick", at = @At(value = "CONSTANT", args = "intValue=8"), cancellable = true)
    private void arclight$cactusRate(BlockState state, ServerLevel level, BlockPos pos, RandomSource random, CallbackInfo ci) {
        if (!arclight$allowCactus(((WorldBridge) level).bridge$spigotConfig().cactusModifier, random)) {
            ci.cancel();
        }
    }

    private static boolean arclight$allowCactus(int modifier, RandomSource random) {
        return modifier == 100 || random.nextFloat() < modifier / (100.0f * 16);
    }

    @Eject(method = "randomTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;setBlockAndUpdate(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Z"))
    private boolean arclight$blockGrow(ServerLevel serverWorld, BlockPos pos, BlockState state, CallbackInfo ci) {
        if (!CraftEventFactory.handleBlockGrowEvent(serverWorld, pos, state, Block.UPDATE_ALL)) {
            ci.cancel();
        }
        return true;
    }
}
