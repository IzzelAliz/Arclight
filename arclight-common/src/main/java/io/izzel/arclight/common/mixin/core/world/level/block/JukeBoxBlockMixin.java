package io.izzel.arclight.common.mixin.core.world.level.block;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.JukeboxBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(JukeboxBlock.class)
public class JukeBoxBlockMixin {

    @ModifyArg(method = "setRecord", index = 0, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/entity/JukeboxBlockEntity;setRecord(Lnet/minecraft/world/item/ItemStack;)V"))
    private ItemStack arclight$oneItem(ItemStack stack) {
        if (!stack.isEmpty()) {
            stack.setCount(1);
        }
        return stack;
    }
}
