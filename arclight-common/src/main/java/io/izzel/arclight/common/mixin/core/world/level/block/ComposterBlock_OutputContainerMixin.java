package io.izzel.arclight.common.mixin.core.world.level.block;

import io.izzel.arclight.common.mixin.core.world.SimpleContainerMixin;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.ComposterBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.craftbukkit.v.inventory.CraftBlockInventoryHolder;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ComposterBlock.OutputContainer.class)
public abstract class ComposterBlock_OutputContainerMixin extends SimpleContainerMixin {

    // @formatter:off
    @Shadow @Final private BlockState state;
    @Shadow @Final private LevelAccessor level;
    @Shadow @Final private BlockPos pos;
    @Shadow private boolean changed;
    // @formatter:on

    @Inject(method = "<init>(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/LevelAccessor;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/item/ItemStack;)V", at = @At("RETURN"))
    public void arclight$setOwner(BlockState blockState, LevelAccessor world, BlockPos blockPos, ItemStack itemStack, CallbackInfo ci) {
        this.setOwner(new CraftBlockInventoryHolder(world, blockPos, this));
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public void setChanged() {
        if (this.isEmpty()) {
            ComposterBlock.empty(this.state, this.level, this.pos);
            this.changed = true;
        } else {
            this.level.setBlock(this.pos, this.state, 3);
            this.changed = false;
        }
    }
}
