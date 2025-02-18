package io.izzel.arclight.fabric.mixin.core.world.level.block.entity;

import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import io.izzel.arclight.mixin.Local;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v.block.CraftBlock;
import org.bukkit.craftbukkit.v.inventory.CraftItemStack;
import org.bukkit.event.inventory.FurnaceSmeltEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(AbstractFurnaceBlockEntity.class)
public abstract class AbstractFurnaceBlockEntityMixin_Fabric {

    @Decorate(method = "burn", at = @At(value = "INVOKE", ordinal = 1, target = "Lnet/minecraft/core/NonNullList;get(I)Ljava/lang/Object;"))
    private static <E> E arclight$furnaceSmelt(NonNullList<E> instance, int i, @Local(ordinal = 0) AbstractFurnaceBlockEntity blockEntity, @Local(ordinal = -1) ItemStack itemStack2, @Local(ordinal = -2) ItemStack itemStack1) throws Throwable {
        CraftItemStack source = CraftItemStack.asCraftMirror(itemStack1);
        org.bukkit.inventory.ItemStack result = CraftItemStack.asBukkitCopy(itemStack2);

        FurnaceSmeltEvent furnaceSmeltEvent = new FurnaceSmeltEvent(CraftBlock.at(blockEntity.getLevel(), blockEntity.getBlockPos()), source, result);
        Bukkit.getPluginManager().callEvent(furnaceSmeltEvent);

        if (furnaceSmeltEvent.isCancelled()) {
            return (E) DecorationOps.cancel().invoke(false);
        }

        result = furnaceSmeltEvent.getResult();
        itemStack2 = CraftItemStack.asNMSCopy(result);
        if (itemStack2.isEmpty()) {
            itemStack1.shrink(1);
            return (E) DecorationOps.cancel().invoke(true);
        }
        return (E) DecorationOps.callsite().invoke(instance, i);
    }
}
