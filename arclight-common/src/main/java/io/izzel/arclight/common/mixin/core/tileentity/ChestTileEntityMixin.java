package io.izzel.arclight.common.mixin.core.tileentity;

import io.izzel.arclight.common.bridge.inventory.IInventoryBridge;
import org.bukkit.craftbukkit.v.entity.CraftHumanEntity;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.bukkit.entity.HumanEntity;
import org.bukkit.inventory.InventoryHolder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;

@Mixin(ChestBlockEntity.class)
public abstract class ChestTileEntityMixin extends LockableTileEntityMixin {

    // @formatter:off
    @Shadow private NonNullList<ItemStack> items;
    @Shadow public int openCount;
    // @formatter:on

    public List<HumanEntity> transaction = new ArrayList<>();
    private int maxStack = IInventoryBridge.MAX_STACK;
    public boolean opened;

    @Inject(method = "startOpen", cancellable = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/entity/ChestBlockEntity;signalOpenCount()V"))
    public void arclight$openRedstone(Player player, CallbackInfo ci) {
        if (this.level == null) {
            ci.cancel();
            return;
        }
        int oldPower = Mth.clamp(this.openCount - 1, 0, 15);
        if (this.getBlockState().getBlock() == Blocks.TRAPPED_CHEST) {
            int newPower = Mth.clamp(this.openCount, 0, 15);

            if (oldPower != newPower) {
                CraftEventFactory.callRedstoneChange(level, worldPosition, oldPower, newPower);
            }
        }
    }

    @Inject(method = "stopOpen", cancellable = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/entity/ChestBlockEntity;signalOpenCount()V"))
    public void arclight$closeRedstone(Player player, CallbackInfo ci) {
        int oldPower = Mth.clamp(this.openCount + 1, 0, 15);
        if (this.getBlockState().getBlock() == Blocks.TRAPPED_CHEST) {
            int newPower = Mth.clamp(this.openCount, 0, 15);

            if (oldPower != newPower) {
                CraftEventFactory.callRedstoneChange(level, worldPosition, oldPower, newPower);
            }
        }
    }

    @Inject(method = "tick", cancellable = true, at = @At(value = "FIELD", shift = At.Shift.AFTER, target = "Lnet/minecraft/world/level/block/entity/ChestBlockEntity;oOpenness:F"))
    private void arclight$openByApi(CallbackInfo ci) {
        if (opened) {
            this.openCount--;
            ci.cancel();
        }
    }

    @Redirect(method = "signalOpenCount", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;blockEvent(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/Block;II)V"))
    private void arclight$soundIfByPlayer(Level world, BlockPos pos, Block blockIn, int eventID, int eventParam) {
        if (!opened) world.blockEvent(pos, blockIn, eventID, eventParam);
    }

    @Override
    public List<ItemStack> getContents() {
        return this.items;
    }

    @Override
    public void onOpen(CraftHumanEntity who) {
        transaction.add(who);
    }

    @Override
    public void onClose(CraftHumanEntity who) {
        transaction.remove(who);
    }

    @Override
    public List<HumanEntity> getViewers() {
        return transaction;
    }

    @Override
    public void setOwner(InventoryHolder owner) {
    }

    @Override
    public int getMaxStackSize() {
        if (maxStack == 0) maxStack = IInventoryBridge.MAX_STACK;
        return maxStack;
    }

    @Override
    public void setMaxStackSize(int size) {
        this.maxStack = size;
    }

    @Override
    public boolean onlyOpsCanSetNbt() {
        return true;
    }
}
