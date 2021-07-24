package io.izzel.arclight.common.mixin.core.tileentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.world.Container;
import net.minecraft.world.Containers;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.CampfireBlockEntity;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v.block.CraftBlock;
import org.bukkit.craftbukkit.v.inventory.CraftItemStack;
import org.bukkit.event.block.BlockCookEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(CampfireBlockEntity.class)
public abstract class CampfireTileEntityMixin extends BlockEntity {

    // @formatter:off
    @Shadow @Final private NonNullList<ItemStack> items;
    @Shadow @Final public int[] cookingProgress;
    @Shadow @Final public int[] cookingTime;
    @Shadow protected abstract void markUpdated();
    // @formatter:on

    public CampfireTileEntityMixin(BlockEntityType<?> tileEntityTypeIn) {
        super(tileEntityTypeIn);
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    private void cook() {
        for (int i = 0; i < this.items.size(); ++i) {
            ItemStack before = this.items.get(i);
            if (!before.isEmpty()) {
                ++this.cookingProgress[i];
                if (this.cookingProgress[i] >= this.cookingTime[i]) {
                    Container iinventory = new SimpleContainer(before);
                    ItemStack after = this.level.getRecipeManager().getRecipeFor(RecipeType.CAMPFIRE_COOKING, iinventory,
                        this.level).map((cookingRecipe) -> cookingRecipe.assemble(iinventory)).orElse(before);
                    BlockPos blockpos = this.getBlockPos();

                    CraftItemStack craftBefore = CraftItemStack.asCraftMirror(before);
                    org.bukkit.inventory.ItemStack bukkitAfter = CraftItemStack.asBukkitCopy(after);
                    BlockCookEvent event = new BlockCookEvent(CraftBlock.at(this.level, this.worldPosition), craftBefore, bukkitAfter);
                    Bukkit.getPluginManager().callEvent(event);
                    if (event.isCancelled()) {
                        continue;
                    }
                    ItemStack cookFinal = CraftItemStack.asNMSCopy(event.getResult());

                    Containers.dropItemStack(this.level, blockpos.getX(), blockpos.getY(), blockpos.getZ(), cookFinal);
                    this.items.set(i, ItemStack.EMPTY);
                    this.markUpdated();
                }
            }
        }

    }
}
