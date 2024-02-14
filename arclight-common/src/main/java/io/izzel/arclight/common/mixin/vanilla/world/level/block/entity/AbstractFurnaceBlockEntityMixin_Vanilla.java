package io.izzel.arclight.common.mixin.vanilla.world.level.block.entity;

import io.izzel.arclight.common.mod.util.ArclightCaptures;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v.block.CraftBlock;
import org.bukkit.craftbukkit.v.inventory.CraftItemStack;
import org.bukkit.event.inventory.FurnaceSmeltEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import javax.annotation.Nullable;

@Mixin(AbstractFurnaceBlockEntity.class)
public abstract class AbstractFurnaceBlockEntityMixin_Vanilla {

    // @formatter:off
    @Shadow private static boolean canBurn(RegistryAccess p_266924_, @org.jetbrains.annotations.Nullable RecipeHolder<?> p_155006_, NonNullList<ItemStack> p_155007_, int p_155008_) { return false; }
    // @formatter:on

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    private static boolean burn(RegistryAccess registryAccess, @Nullable RecipeHolder<?> recipe, NonNullList<ItemStack> items, int i) {
        if (recipe != null && canBurn(registryAccess, recipe, items, i)) {
            ItemStack itemstack = items.get(0);
            ItemStack itemstack1 = recipe.value().getResultItem(registryAccess);
            ItemStack itemstack2 = items.get(2);

            if (ArclightCaptures.getTickingBlockEntity() != null) {
                var blockEntity = ArclightCaptures.getTickingBlockEntity();
                CraftItemStack source = CraftItemStack.asCraftMirror(itemstack);
                org.bukkit.inventory.ItemStack result = CraftItemStack.asBukkitCopy(itemstack1);

                FurnaceSmeltEvent furnaceSmeltEvent = new FurnaceSmeltEvent(CraftBlock.at(blockEntity.getLevel(), blockEntity.getBlockPos()), source, result);
                Bukkit.getPluginManager().callEvent(furnaceSmeltEvent);

                if (furnaceSmeltEvent.isCancelled()) {
                    return false;
                }

                result = furnaceSmeltEvent.getResult();
                itemstack1 = CraftItemStack.asNMSCopy(result);
            }

            if (!itemstack1.isEmpty()) {
                if (itemstack2.isEmpty()) {
                    items.set(2, itemstack1.copy());
                } else if (ItemStack.isSameItemSameTags(itemstack2, itemstack1)) {
                    itemstack2.grow(itemstack1.getCount());
                } else {
                    return false;
                }
            }

            if (itemstack.is(Blocks.WET_SPONGE.asItem()) && !items.get(1).isEmpty() && items.get(1).is(Items.BUCKET)) {
                items.set(1, new ItemStack(Items.WATER_BUCKET));
            }

            itemstack.shrink(1);
            return true;
        } else {
            return false;
        }
    }
}
