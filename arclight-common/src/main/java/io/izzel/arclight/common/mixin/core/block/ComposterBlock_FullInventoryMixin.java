package io.izzel.arclight.common.mixin.core.block;

import io.izzel.arclight.common.mixin.core.inventory.InventoryMixin;
import net.minecraft.block.BlockState;
import net.minecraft.block.ComposterBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IWorld;
import org.bukkit.craftbukkit.v.inventory.CraftBlockInventoryHolder;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.block.ComposterBlock$FullInventory")
public abstract class ComposterBlock_FullInventoryMixin extends InventoryMixin {

    // @formatter:off
    @Shadow @Final private BlockState state;
    @Shadow @Final private IWorld world;
    @Shadow @Final private BlockPos pos;
    @Shadow private boolean extracted;
    // @formatter:on

    @Inject(method = "<init>(Lnet/minecraft/block/BlockState;Lnet/minecraft/world/IWorld;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/item/ItemStack;)V", at = @At("RETURN"))
    public void arclight$setOwner(BlockState blockState, IWorld world, BlockPos blockPos, ItemStack itemStack, CallbackInfo ci) {
        this.setOwner(new CraftBlockInventoryHolder(world, blockPos, this));
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public void markDirty() {
        if (this.isEmpty()) {
            ComposterBlock.resetFillState(this.state, this.world, this.pos);
            this.extracted = true;
        } else {
            this.world.setBlockState(this.pos, this.state, 3);
            this.extracted = false;
        }
    }
}
