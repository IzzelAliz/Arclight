package io.izzel.arclight.common.mixin.core.world.level.block;

import io.izzel.arclight.api.ArclightPlatform;
import io.izzel.arclight.common.bridge.core.world.WorldBridge;
import io.izzel.arclight.common.mod.mixins.annotation.OnlyInPlatform;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CarvedPumpkinBlock;
import net.minecraft.world.level.block.WitherSkullBlock;
import net.minecraft.world.level.block.entity.SkullBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.pattern.BlockPattern;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(WitherSkullBlock.class)
public class WitherSkullBlockMixin {

    @Redirect(method = "checkSpawn", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/CarvedPumpkinBlock;clearPatternBlocks(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/level/block/state/pattern/BlockPattern$BlockPatternMatch;)V"))
    private static void arclight$clearLater(Level p_249604_, BlockPattern.BlockPatternMatch p_251190_) {
    }

    @Mixin(WitherSkullBlock.class)
    @OnlyInPlatform({ArclightPlatform.FORGE, ArclightPlatform.NEOFORGE})
    public static class ForgeLike {

        @Inject(method = "checkSpawn", cancellable = true, locals = LocalCapture.CAPTURE_FAILHARD, at = @At(value = "INVOKE", shift = At.Shift.AFTER, target = "Lnet/minecraft/world/entity/boss/wither/WitherBoss;makeInvulnerable()V"))
        private static void arclight$addEntity(Level level, BlockPos pos, SkullBlockEntity p_58258_, CallbackInfo ci,
                                               BlockState state, boolean flag, BlockPattern.BlockPatternMatch patternMatch, WitherBoss witherBoss) {
            ((WorldBridge) level).bridge$pushAddEntityReason(CreatureSpawnEvent.SpawnReason.BUILD_WITHER);
            if (!level.addFreshEntity(witherBoss)) {
                ci.cancel();
            } else {
                CarvedPumpkinBlock.clearPatternBlocks(level, patternMatch);
            }
        }
    }

    @Mixin(WitherSkullBlock.class)
    @OnlyInPlatform({ArclightPlatform.VANILLA, ArclightPlatform.FABRIC})
    public static class VanillaLike {

        @Inject(method = "checkSpawn", cancellable = true, locals = LocalCapture.CAPTURE_FAILHARD, at = @At(value = "INVOKE", shift = At.Shift.AFTER, target = "Lnet/minecraft/world/entity/boss/wither/WitherBoss;makeInvulnerable()V"))
        private static void arclight$addEntity(Level level, BlockPos pos, SkullBlockEntity p_58258_, CallbackInfo ci,
                                               boolean flag, BlockPattern.BlockPatternMatch patternMatch, WitherBoss witherBoss) {
            ((WorldBridge) level).bridge$pushAddEntityReason(CreatureSpawnEvent.SpawnReason.BUILD_WITHER);
            if (!level.addFreshEntity(witherBoss)) {
                ci.cancel();
            } else {
                CarvedPumpkinBlock.clearPatternBlocks(level, patternMatch);
            }
        }
    }

    @Redirect(method = "checkSpawn", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"))
    private static boolean arclight$muteSpawn(Level instance, Entity entity) {
        return true;
    }
}
