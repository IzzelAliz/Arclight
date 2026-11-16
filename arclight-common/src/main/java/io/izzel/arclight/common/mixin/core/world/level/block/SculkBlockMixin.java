package io.izzel.arclight.common.mixin.core.world.level.block;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SculkBlock;
import net.minecraft.world.level.block.SculkSpreader;
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(SculkBlock.class)
public class SculkBlockMixin {

    @Redirect(method = "attemptUseCharge", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/LevelAccessor;setBlockAndUpdate(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Z"))
    private boolean arclight$blockSpread(LevelAccessor level, BlockPos pos, BlockState state,
                                         SculkSpreader.ChargeCursor cursor, LevelAccessor sourceLevel, BlockPos source,
                                         net.minecraft.util.RandomSource random, SculkSpreader spreader, boolean spreadVein,
                                         @Share("arclight$spreadAccepted") LocalBooleanRef spreadAccepted) {
        boolean accepted = CraftEventFactory.handleBlockSpreadEvent(level, source, pos, state, Block.UPDATE_ALL);
        spreadAccepted.set(accepted);
        return accepted;
    }

    @WrapOperation(method = "attemptUseCharge", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/LevelAccessor;playSound(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/core/BlockPos;Lnet/minecraft/sounds/SoundEvent;Lnet/minecraft/sounds/SoundSource;FF)V"), require = 1, allow = 1)
    private void arclight$spreadSound(LevelAccessor level, Entity entity, BlockPos pos, SoundEvent sound,
                                      SoundSource source, float volume, float pitch, Operation<Void> original,
                                      @Share("arclight$spreadAccepted") LocalBooleanRef spreadAccepted) {
        if (spreadAccepted.get()) {
            original.call(level, entity, pos, sound, source, volume, pitch);
        }
    }
}
