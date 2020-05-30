package io.izzel.arclight.common.mixin.core.tileentity;

import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.InventoryHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipeType;
import net.minecraft.tileentity.CampfireTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v.block.CraftBlock;
import org.bukkit.craftbukkit.v.inventory.CraftItemStack;
import org.bukkit.event.block.BlockCookEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(CampfireTileEntity.class)
public abstract class CampfireTileEntityMixin extends TileEntity {

    // @formatter:off
    @Shadow @Final private NonNullList<ItemStack> inventory;
    @Shadow @Final public int[] cookingTimes;
    @Shadow @Final public int[] cookingTotalTimes;
    @Shadow protected abstract void inventoryChanged();
    // @formatter:on

    public CampfireTileEntityMixin(TileEntityType<?> tileEntityTypeIn) {
        super(tileEntityTypeIn);
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    private void cookAndDrop() {
        for (int i = 0; i < this.inventory.size(); ++i) {
            ItemStack before = this.inventory.get(i);
            if (!before.isEmpty()) {
                ++this.cookingTimes[i];
                if (this.cookingTimes[i] >= this.cookingTotalTimes[i]) {
                    IInventory iinventory = new Inventory(before);
                    ItemStack after = this.world.getRecipeManager().getRecipe(IRecipeType.CAMPFIRE_COOKING, iinventory,
                        this.world).map((cookingRecipe) -> cookingRecipe.getCraftingResult(iinventory)).orElse(before);
                    BlockPos blockpos = this.getPos();

                    CraftItemStack craftBefore = CraftItemStack.asCraftMirror(before);
                    org.bukkit.inventory.ItemStack bukkitAfter = CraftItemStack.asBukkitCopy(after);
                    BlockCookEvent event = new BlockCookEvent(CraftBlock.at(this.world, this.pos), craftBefore, bukkitAfter);
                    Bukkit.getPluginManager().callEvent(event);
                    if (event.isCancelled()) {
                        continue;
                    }
                    ItemStack cookFinal = CraftItemStack.asNMSCopy(event.getResult());

                    InventoryHelper.spawnItemStack(this.world, blockpos.getX(), blockpos.getY(), blockpos.getZ(), cookFinal);
                    this.inventory.set(i, ItemStack.EMPTY);
                    this.inventoryChanged();
                }
            }
        }

    }
}
