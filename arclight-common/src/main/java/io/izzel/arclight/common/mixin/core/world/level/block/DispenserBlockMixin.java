package io.izzel.arclight.common.mixin.core.world.level.block;

import io.izzel.arclight.common.mod.mixins.annotation.TransformAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DispenserBlock.class)
public class DispenserBlockMixin {

    @TransformAccess(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC)
    private static boolean eventFired = false;

    @Inject(method = "dispenseFrom", at = @At(value = "INVOKE", target = "Lnet/minecraft/core/dispenser/DispenseItemBehavior;dispense(Lnet/minecraft/core/dispenser/BlockSource;Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/world/item/ItemStack;"))
    private void arclight$reset(ServerLevel serverLevel, BlockState blockState, BlockPos blockPos, CallbackInfo ci) {
        eventFired = false;
    }
}
