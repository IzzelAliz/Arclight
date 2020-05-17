package io.izzel.arclight.mixin.core.block;

import io.izzel.arclight.mixin.core.inventory.InventoryMixin;
import net.minecraft.block.BlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IWorld;
import org.bukkit.craftbukkit.v1_14_R1.inventory.CraftBlockInventoryHolder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.block.ComposterBlock$FullInventory")
public abstract class ComposterBlock_FullInventoryMixin extends InventoryMixin {

    @Inject(method = "<init>(Lnet/minecraft/block/BlockState;Lnet/minecraft/world/IWorld;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/item/ItemStack;)V", at = @At("RETURN"))
    public void arclight$setOwner(BlockState blockState, IWorld world, BlockPos blockPos, ItemStack itemStack, CallbackInfo ci) {
        this.setOwner(new CraftBlockInventoryHolder(world, blockPos, this));
    }
}
