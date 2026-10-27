package io.izzel.arclight.forge.mixin.core.world.level.block.entity;

import io.izzel.arclight.forge.mod.util.DelegatedContainer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(HopperBlockEntity.class)
public class HopperBlockEntityMixin_Forge {

    // Somehow common mixin can inject into forge one
    // Eject can only be applied once, don't apply here
    // @Eject(method = "ejectItems", remap = false, at = @At(value = "INVOKE", remap = true, target = "Lnet/minecraft/world/level/block/entity/HopperBlockEntity;addItem(Lnet/minecraft/world/Container;Lnet/minecraft/world/Container;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/core/Direction;)Lnet/minecraft/world/item/ItemStack;"))
    // private static ItemStack arclight$moveItem(Container source, Container destination, ItemStack stack, Direction direction, CallbackInfoReturnable<Boolean> cir)
    // Content is identical to the corresponding common mixin

    @Inject(method = "ejectItems", cancellable = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/entity/HopperBlockEntity;getAttachedContainer(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/entity/HopperBlockEntity;)Lnet/minecraft/world/Container;"))
    private static void arclight$cancelIfFound(Level level, BlockPos blockPos, HopperBlockEntity hopperBlockEntity, CallbackInfoReturnable<Boolean> cir) {
        if (DelegatedContainer.foundLastHandler()) {
            cir.setReturnValue(false);
        }
    }
}
