package io.izzel.arclight.common.mixin.core.world.level.block;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.izzel.arclight.common.mod.server.block.CauldronHooks;
import io.izzel.arclight.mixin.Eject;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.InsideBlockEffectType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.event.block.CauldronLevelChangeEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Consumer;

@Mixin(LayeredCauldronBlock.class)
public class LayeredCauldronBlockMixin {

    @Redirect(method = "entityInside(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/entity/InsideBlockEffectApplier;Z)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/InsideBlockEffectApplier;apply(Lnet/minecraft/world/entity/InsideBlockEffectType;)V"))
    private void arclight$extinguish1(InsideBlockEffectApplier applier, InsideBlockEffectType type, BlockState state, Level level, BlockPos pos, Entity entity) {
        if (type != InsideBlockEffectType.EXTINGUISH) {
            applier.apply(type);
        }
    }

    @Redirect(method = "entityInside(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/entity/InsideBlockEffectApplier;Z)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/InsideBlockEffectApplier;runBefore(Lnet/minecraft/world/entity/InsideBlockEffectType;Ljava/util/function/Consumer;)V"))
    private void arclight$extinguish2(InsideBlockEffectApplier applier, InsideBlockEffectType type, Consumer<Entity> effect, BlockState state, Level level, BlockPos pos, Entity entity) {
        // Snapshot at registration time: the consumer runs later, after the caller may reuse a mutable cursor.
        BlockPos immutablePos = pos.immutable();
        applier.runBefore(type, e -> {
            if (type == InsideBlockEffectType.EXTINGUISH) {
                // Let the native consumer perform its isOnFire/mayInteract guard exactly once.
                // Fixed CraftBukkit checks e but uses the original entity argument as event actor.
                boolean extinguishAccepted = CauldronHooks.runWithChangeInvocation(
                    entity,
                    immutablePos,
                    () -> effect.accept(e)
                );
                if (extinguishAccepted) {
                    // Preserve current immediate-clear timing; deferred apply remains unselected.
                    e.clearFire();
                }
            } else {
                effect.accept(e);
            }
        });
    }

    @WrapOperation(method = "lambda$entityInside$0(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/Entity;)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/LayeredCauldronBlock;handleEntityOnFireInside(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)V"),
        require = 1, expect = 1, allow = 1)
    private void arclight$cauldronActionContext(LayeredCauldronBlock block, BlockState state, Level level, BlockPos pos, Operation<Void> original) {
        CauldronHooks.runWithChangeContextForCurrentInvocation(() -> original.call(block, state, level, pos));
    }

    @io.izzel.arclight.mixin.Decorate(method = "lowerFillLevel(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;setBlockAndUpdate(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Z"))
    private static boolean arclight$lowerFill(Level level, BlockPos pos, BlockState state, BlockState old) throws Throwable {
        BlockState prepared = CauldronHooks.takePreparedInteraction(level, pos);
        if (prepared != null) {
            return (boolean) io.izzel.arclight.mixin.DecorationOps.callsite().invoke(level, pos, prepared);
        }
        if (!CauldronHooks.changeLevel(old, level, pos, state, CauldronHooks.getEntity(), CauldronHooks.getReason())) {
            return (boolean) io.izzel.arclight.mixin.DecorationOps.cancel().invoke();
        }
        return true;
    }

    @Eject(method = "handlePrecipitation(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/biome/Biome$Precipitation;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;setBlockAndUpdate(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Z"))
    private boolean arclight$precipitation(Level level, BlockPos pos, BlockState state, CallbackInfo ci, BlockState old) {
        if (!CauldronHooks.changeLevel(old, level, pos, state, null, CauldronLevelChangeEvent.ChangeReason.NATURAL_FILL)) {
            ci.cancel();
            return false;
        }
        return true;
    }

    @Eject(method = "receiveStalactiteDrip", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;setBlockAndUpdate(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Z"))
    private boolean arclight$drip(Level level, BlockPos pos, BlockState state, CallbackInfo ci, BlockState old) {
        if (CauldronHooks.changeLevel(old, level, pos, state, null, CauldronLevelChangeEvent.ChangeReason.NATURAL_FILL)) {
            return true;
        } else {
            ci.cancel();
            return false;
        }
    }
}
