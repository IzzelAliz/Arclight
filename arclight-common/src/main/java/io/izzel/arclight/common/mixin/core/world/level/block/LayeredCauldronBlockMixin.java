package io.izzel.arclight.common.mixin.core.world.level.block;

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
        applier.runBefore(type, e -> {
            CauldronHooks.setChangeReason(e, CauldronLevelChangeEvent.ChangeReason.EXTINGUISH);
            effect.accept(e);
            if (CauldronHooks.getResult()) {
                e.clearFire();
            }
            CauldronHooks.reset();
        });
    }

    @Redirect(method = "lowerFillLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;setBlockAndUpdate(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Z"))
    private static boolean arclight$lowerFill(Level level, BlockPos pos, BlockState state, BlockState old) {
        return CauldronHooks.changeLevel(old, level, pos, state, CauldronHooks.getEntity(), CauldronHooks.getReason());
    }

    @Redirect(method = "handlePrecipitation", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;setBlockAndUpdate(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Z"))
    private boolean arclight$precipitation(Level level, BlockPos pos, BlockState state, BlockState old) {
        return CauldronHooks.changeLevel(old, level, pos, state, null, CauldronLevelChangeEvent.ChangeReason.NATURAL_FILL);
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
