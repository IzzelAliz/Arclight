package io.izzel.arclight.forge.mixin.core.world.level.block.entity;

import net.minecraft.world.level.block.entity.HopperBlockEntity;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(HopperBlockEntity.class)
public class HopperBlockEntityMixin_Forge {

    // Somehow common mixin can inject into forge one
    // Eject can only be applied once, don't apply here
    // @Eject(method = "ejectItems", remap = false, at = @At(value = "INVOKE", remap = true, target = "Lnet/minecraft/world/level/block/entity/HopperBlockEntity;addItem(Lnet/minecraft/world/Container;Lnet/minecraft/world/Container;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/core/Direction;)Lnet/minecraft/world/item/ItemStack;"))
    // private static ItemStack arclight$moveItem(Container source, Container destination, ItemStack stack, Direction direction, CallbackInfoReturnable<Boolean> cir)
    // Content is identical to the corresponding common mixin
}
