package io.izzel.arclight.common.mixin.optimization.general.chunkload;

import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.SleepInBed;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(SleepInBed.class)
public class SleepInBedMixin_Optimize {

    @Inject(method = "checkExtraStartConditions",cancellable = true, locals = LocalCapture.CAPTURE_FAILHARD, at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;"))
    private void arclight$returnIfNotLoaded(ServerLevel level, LivingEntity entity, CallbackInfoReturnable<Boolean> cir, Brain<?> brain, GlobalPos pos) {
        if (!level.isLoaded(pos.pos())) {
            cir.setReturnValue(false);
        }
    }
}
