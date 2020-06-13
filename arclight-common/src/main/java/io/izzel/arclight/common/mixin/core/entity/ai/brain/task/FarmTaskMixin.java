package io.izzel.arclight.common.mixin.core.entity.ai.brain.task;

import net.minecraft.entity.ai.brain.task.FarmTask;
import net.minecraft.entity.merchant.villager.VillagerEntity;
import net.minecraft.world.server.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import io.izzel.arclight.common.mod.util.ArclightCaptures;

@Mixin(FarmTask.class)
public abstract class FarmTaskMixin {

    @Inject(method = "updateTask", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/server/ServerWorld;setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;I)Z"))
    private void on(ServerWorld worldIn, VillagerEntity owner, long gameTime, CallbackInfo ci) {
        ArclightCaptures.captureEntityChangeBlock(owner);
    }

}
