package io.izzel.arclight.common.mixin.core.tileentity;

import io.izzel.arclight.common.bridge.inventory.IInventoryBridge;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.ChestTileEntity;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.MathHelper;
import org.bukkit.craftbukkit.v.entity.CraftHumanEntity;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.bukkit.entity.HumanEntity;
import org.bukkit.inventory.InventoryHolder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

@Mixin(ChestTileEntity.class)
public abstract class ChestTileEntityMixin extends LockableTileEntityMixin {

    // @formatter:off
    @Shadow private NonNullList<ItemStack> chestContents;
    @Shadow protected int numPlayersUsing;
    // @formatter:on

    public List<HumanEntity> transaction = new ArrayList<>();
    private int maxStack = IInventoryBridge.MAX_STACK;

    @Inject(method = "openInventory", cancellable = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/tileentity/ChestTileEntity;onOpenOrClose()V"))
    public void arclight$openRedstone(PlayerEntity player, CallbackInfo ci) {
        if (this.world == null) {
            ci.cancel();
            return;
        }
        int oldPower = MathHelper.clamp(this.numPlayersUsing - 1, 0, 15);
        if (this.getBlockState().getBlock() == Blocks.TRAPPED_CHEST) {
            int newPower = MathHelper.clamp(this.numPlayersUsing, 0, 15);

            if (oldPower != newPower) {
                CraftEventFactory.callRedstoneChange(world, pos, oldPower, newPower);
            }
        }
    }

    @Inject(method = "closeInventory", cancellable = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/tileentity/ChestTileEntity;onOpenOrClose()V"))
    public void arclight$closeRedstone(PlayerEntity player, CallbackInfo ci) {
        int oldPower = MathHelper.clamp(this.numPlayersUsing + 1, 0, 15);
        if (this.getBlockState().getBlock() == Blocks.TRAPPED_CHEST) {
            int newPower = MathHelper.clamp(this.numPlayersUsing, 0, 15);

            if (oldPower != newPower) {
                CraftEventFactory.callRedstoneChange(world, pos, oldPower, newPower);
            }
        }
    }

    @Override
    public List<ItemStack> getContents() {
        return this.chestContents;
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
    public int getInventoryStackLimit() {
        if (maxStack == 0) maxStack = IInventoryBridge.MAX_STACK;
        return maxStack;
    }

    @Override
    public void setMaxStackSize(int size) {
        this.maxStack = size;
    }

    public boolean onlyOpsCanSetNbt() {
        return true;
    }
}
