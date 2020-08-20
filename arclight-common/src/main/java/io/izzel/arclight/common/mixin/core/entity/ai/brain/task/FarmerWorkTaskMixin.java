package io.izzel.arclight.common.mixin.core.entity.ai.brain.task;

import io.izzel.arclight.common.mod.util.ArclightCaptures;
import net.minecraft.block.BlockState;
import net.minecraft.entity.ai.brain.task.FarmerWorkTask;
import net.minecraft.entity.merchant.villager.VillagerEntity;
import net.minecraft.util.math.GlobalPos;
import net.minecraft.world.server.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FarmerWorkTask.class)
public class FarmerWorkTaskMixin {

    @Inject(method = "func_234016_a_", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/ComposterBlock;empty(Lnet/minecraft/block/BlockState;Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;)Lnet/minecraft/block/BlockState;"))
    private static void arclight$captureVillager1(ServerWorld p_234016_1_, VillagerEntity p_234016_2_, GlobalPos p_234016_3_, BlockState p_234016_4_, CallbackInfo ci) {
        ArclightCaptures.captureEntityChangeBlock(p_234016_2_);
    }

    @Inject(method = "func_234016_a_", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/ComposterBlock;attemptFill(Lnet/minecraft/block/BlockState;Lnet/minecraft/world/server/ServerWorld;Lnet/minecraft/item/ItemStack;Lnet/minecraft/util/math/BlockPos;)Lnet/minecraft/block/BlockState;"))
    private static void arclight$captureVillager2(ServerWorld p_234016_1_, VillagerEntity p_234016_2_, GlobalPos p_234016_3_, BlockState p_234016_4_, CallbackInfo ci) {
        ArclightCaptures.captureEntityChangeBlock(p_234016_2_);
    }
}
