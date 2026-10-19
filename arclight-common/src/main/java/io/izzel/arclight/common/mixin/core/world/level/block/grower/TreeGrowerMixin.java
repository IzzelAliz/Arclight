package io.izzel.arclight.common.mixin.core.world.level.block.grower;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.sugar.Local;
import io.izzel.arclight.common.mod.util.TreeGrowthContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TreeGrower.class)
public abstract class TreeGrowerMixin {
    @ModifyExpressionValue(method = "growTree", at = @At(value = "INVOKE", target = "Lnet/neoforged/neoforge/event/level/BlockGrowFeatureEvent;isCanceled()Z", ordinal = 0), require = 0)
    private boolean arclight$megaGuard(boolean cancelled, @Local Holder<ConfiguredFeature<?, ?>> holder) {
        return cancelled;
    }
    @ModifyExpressionValue(method = "growTree", at = @At(value = "INVOKE", target = "Lnet/neoforged/neoforge/event/level/BlockGrowFeatureEvent;isCanceled()Z", ordinal = 1), require = 0)
    private boolean arclight$ordinaryGuard(boolean cancelled, @Local Holder<ConfiguredFeature<?, ?>> holder) {
        return cancelled;
    }
    @WrapMethod(method = "growTree")
    private boolean arclight$scopedType(ServerLevel level, ChunkGenerator generator, BlockPos pos,
        BlockState state, RandomSource random, Operation<Boolean> original) {
        try (var frame = TreeGrowthContext.enter(level)) {
            boolean result = original.call(level, generator, pos, state, random);
            frame.returnedNormally();
            return result;
        }
    }
    // First scan call is after nonnull mega Holder and, on Neo, replacement/cancel/null guards.
    // Record once before native scan reads; retain the fixed caller's mega value if ordinary has no Holder.
    @Inject(method = "growTree", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/grower/TreeGrower;isTwoByTwoSapling(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;II)Z"))
    private void arclight$megaType(ServerLevel level, ChunkGenerator generator, BlockPos pos,
        BlockState state, RandomSource random, CallbackInfoReturnable<Boolean> cir,
        @Local Holder<ConfiguredFeature<?, ?>> holder) {
        TreeGrowthContext.selected(level, TreeGrowthContext.Site.MEGA, holder);
    }
    @Inject(method = "growTree", at = @At(value = "INVOKE", target = "Lnet/minecraft/core/Holder;value()Ljava/lang/Object;", ordinal = 1))
    private void arclight$ordinaryType(ServerLevel level, ChunkGenerator generator, BlockPos pos,
        BlockState state, RandomSource random, CallbackInfoReturnable<Boolean> cir,
        @Local Holder<ConfiguredFeature<?, ?>> holder) {
        TreeGrowthContext.selected(level, TreeGrowthContext.Site.ORDINARY, holder);
    }
}
