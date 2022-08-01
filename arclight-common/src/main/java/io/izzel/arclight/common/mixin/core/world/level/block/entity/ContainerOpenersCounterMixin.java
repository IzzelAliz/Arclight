package io.izzel.arclight.common.mixin.core.world.level.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.ContainerOpenersCounter;
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ContainerOpenersCounter.class)
public abstract class ContainerOpenersCounterMixin {

    // @formatter:off
    @Shadow private int openCount;
    @Shadow protected abstract void onOpen(Level p_155460_, BlockPos p_155461_, BlockState p_155462_);
    @Shadow protected abstract void onClose(Level p_155473_, BlockPos p_155474_, BlockState p_155475_);
    @Shadow protected abstract void openerCountChanged(Level p_155463_, BlockPos p_155464_, BlockState p_155465_, int p_155466_, int p_155467_);
    // @formatter:on

    public boolean opened;

    public void onAPIOpen(Level world, BlockPos blockposition, BlockState iblockdata) {
        onOpen(world, blockposition, iblockdata);
    }

    public void onAPIClose(Level world, BlockPos blockposition, BlockState iblockdata) {
        onClose(world, blockposition, iblockdata);
    }

    public void openerAPICountChanged(Level world, BlockPos blockposition, BlockState iblockdata, int i, int j) {
        openerCountChanged(world, blockposition, iblockdata, i, j);
    }

    @Inject(method = "incrementOpeners", at = @At("HEAD"))
    private void arclight$increase(Player p_155453_, Level level, BlockPos pos, BlockState p_155456_, CallbackInfo ci) {
        int oldPower = Math.max(0, Math.min(15, this.openCount++));
        if (level.getBlockState(pos).is(net.minecraft.world.level.block.Blocks.TRAPPED_CHEST)) {
            int newPower = Math.max(0, Math.min(15, this.openCount));
            if (oldPower != newPower) {
                CraftEventFactory.callRedstoneChange(level, pos, oldPower, newPower);
            }
        }
        this.openCount--;
    }

    @Inject(method = "decrementOpeners", at = @At("HEAD"))
    private void arclight$decrease(Player p_155453_, Level level, BlockPos pos, BlockState p_155456_, CallbackInfo ci) {
        int oldPower = Math.max(0, Math.min(15, this.openCount--));
        if (level.getBlockState(pos).is(net.minecraft.world.level.block.Blocks.TRAPPED_CHEST)) {
            int newPower = Math.max(0, Math.min(15, this.openCount));
            if (oldPower != newPower) {
                CraftEventFactory.callRedstoneChange(level, pos, oldPower, newPower);
            }
        }
        this.openCount++;
    }

    @ModifyVariable(method = "recheckOpeners", ordinal = 0, at = @At(value = "FIELD", ordinal = 0, target = "Lnet/minecraft/world/level/block/entity/ContainerOpenersCounter;openCount:I"))
    private int arclight$addOpens(int power) {
        return opened ? power + 1 : power;
    }
}
