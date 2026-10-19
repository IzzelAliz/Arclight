package io.izzel.arclight.common.mixin.core.world.level.block.state;

import io.izzel.arclight.common.bridge.core.world.level.ExplosionBridge;
import io.izzel.arclight.common.bridge.core.world.level.block.state.BlockBehaviourBridge;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import io.izzel.arclight.mixin.Local;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.ServerExplosion;
import net.minecraft.util.context.ContextKey;
import org.spongepowered.asm.mixin.injection.Slice;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import java.util.function.BiConsumer;

@Mixin(BlockBehaviour.class)
public abstract class BlockBehaviourMixin implements BlockBehaviourBridge {

    // @formatter:off
    @Shadow
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess scheduledTickAccess,
                                     BlockPos pos, Direction direction, BlockPos neighborPos,
                                     BlockState neighborState, RandomSource random) { return null; }
    // @formatter:on

    // Suppress only the native decay parameter; later mod context writes remain intact.
    @Decorate(method = "onExplosionHit(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/Explosion;Ljava/util/function/BiConsumer;)V", at = @At(value = "INVOKE", ordinal = 0, target = "Lnet/minecraft/world/level/storage/loot/LootParams$Builder;withParameter(Lnet/minecraft/util/context/ContextKey;Ljava/lang/Object;)Lnet/minecraft/world/level/storage/loot/LootParams$Builder;"),
        slice = @Slice(from = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Explosion;radius()F"),
            to = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;spawnAfterBreak(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/item/ItemStack;Z)V")))
    private LootParams.Builder arclight$nativeRadius(LootParams.Builder builder, ContextKey<?> key, Object radius,
                                                   @Local(ordinal = -1) Explosion explosion) throws Throwable {
        if (explosion instanceof ServerExplosion) {
            return builder;
        }
        return (LootParams.Builder) DecorationOps.callsite().invoke(builder, key, radius);
    }

    @Decorate(method = "onExplosionHit(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/Explosion;Ljava/util/function/BiConsumer;)V", inject = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;spawnAfterBreak(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/item/ItemStack;Z)V"))
    private void arclight$setRadius(BlockState blockState, ServerLevel level, BlockPos blockPos, Explosion explosion, BiConsumer<ItemStack, BlockPos> biConsumer,
                                    @Local(ordinal = -1) LootParams.Builder builder) {
        if (!(explosion instanceof ServerExplosion)) {
            return;
        }
        var yield = ((ExplosionBridge) explosion).bridge$getYield();
        if (yield < 1.0F) {
            builder.withParameter(LootContextParams.EXPLOSION_RADIUS, 1.0F / yield);
        }
    }
}
