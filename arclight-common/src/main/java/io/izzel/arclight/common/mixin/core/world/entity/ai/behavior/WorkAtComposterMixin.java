package io.izzel.arclight.common.mixin.core.world.entity.ai.behavior;

import io.izzel.arclight.common.mod.util.ArclightCaptures;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.behavior.WorkAtComposter;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorkAtComposter.class)
public class WorkAtComposterMixin {

    @Inject(method = "compostItems", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/ComposterBlock;extractProduce(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;"))
    private void arclight$captureVillager1(ServerLevel world, Villager villager, GlobalPos p_234016_3_, BlockState state, CallbackInfo ci) {
        ArclightCaptures.captureEntityChangeBlock(villager);
    }

    @Inject(method = "compostItems", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/ComposterBlock;insertItem(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;"))
    private void arclight$captureVillager2(ServerLevel world, Villager villager, GlobalPos p_234016_3_, BlockState state, CallbackInfo ci) {
        ArclightCaptures.captureEntityChangeBlock(villager);
    }
}
