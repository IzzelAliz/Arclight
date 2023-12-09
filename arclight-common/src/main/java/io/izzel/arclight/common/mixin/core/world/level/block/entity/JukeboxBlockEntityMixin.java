package io.izzel.arclight.common.mixin.core.world.level.block.entity;

import io.izzel.arclight.common.bridge.core.inventory.IInventoryBridge;
import io.izzel.arclight.common.bridge.core.world.WorldBridge;
import io.izzel.arclight.common.mod.util.DistValidate;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.JukeboxBlockEntity;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v.entity.CraftHumanEntity;
import org.bukkit.entity.HumanEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Mixin(JukeboxBlockEntity.class)
public abstract class JukeboxBlockEntityMixin extends BlockEntityMixin implements IInventoryBridge, Container {

    @Shadow private ItemStack item;

    public List<HumanEntity> transaction = new ArrayList<>();
    private int maxStack = MAX_STACK;
    public boolean opened;

    @Override
    public int getMaxStackSize() {
        return maxStack;
    }

    @Override
    public List<ItemStack> getContents() {
        return Collections.singletonList(this.item);
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
    public void setMaxStackSize(int size) {
        maxStack = size;
    }

    @Override
    public Location getLocation() {
        if (!DistValidate.isValid(level)) return null;
        return new Location(((WorldBridge) level).bridge$getWorld(), worldPosition.getX(), worldPosition.getY(), worldPosition.getZ());
    }

    @Redirect(method = "setRecordWithoutPlaying", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;updateNeighborsAt(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/Block;)V"))
    private void arclight$levelNullCheck(Level instance, BlockPos p_46673_, Block p_46674_) {
        if (instance != null) {
            instance.updateNeighborsAt(p_46673_, p_46674_);
        }
    }
}
