package io.izzel.arclight.common.mixin.core.block;

import net.minecraft.block.JukeboxBlock;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(JukeboxBlock.class)
public class JukeBoxBlockMixin {

    @ModifyArg(method = "insertRecord", index = 0, at = @At(value = "INVOKE", target = "Lnet/minecraft/tileentity/JukeboxTileEntity;setRecord(Lnet/minecraft/item/ItemStack;)V"))
    private ItemStack arclight$oneItem(ItemStack stack) {
        if (!stack.isEmpty()) {
            stack.setCount(1);
        }
        return stack;
    }
}
