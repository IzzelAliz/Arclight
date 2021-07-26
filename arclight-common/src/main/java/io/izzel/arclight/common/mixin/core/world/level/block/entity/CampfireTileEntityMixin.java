package io.izzel.arclight.common.mixin.core.world.level.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.Containers;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.CampfireBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v.block.CraftBlock;
import org.bukkit.craftbukkit.v.inventory.CraftItemStack;
import org.bukkit.event.block.BlockCookEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(CampfireBlockEntity.class)
public abstract class CampfireTileEntityMixin extends BlockEntityMixin {

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public static void cookTick(Level level, BlockPos pos, BlockState state, CampfireBlockEntity entity) {
        boolean flag = false;

        for (int i = 0; i < entity.getItems().size(); ++i) {
            ItemStack itemstack = entity.getItems().get(i);
            if (!itemstack.isEmpty()) {
                flag = true;
                if (entity.cookingProgress[i] >= entity.cookingTime[i]) {
                    Container container = new SimpleContainer(itemstack);
                    ItemStack itemstack1 = level.getRecipeManager().getRecipeFor(RecipeType.CAMPFIRE_COOKING, container, level).map((p_155305_) -> {
                        return p_155305_.assemble(container);
                    }).orElse(itemstack);
                    CraftItemStack source = CraftItemStack.asCraftMirror(itemstack);
                    org.bukkit.inventory.ItemStack result = CraftItemStack.asBukkitCopy(itemstack1);

                    BlockCookEvent blockCookEvent = new BlockCookEvent(CraftBlock.at(level, pos), source, result);
                    Bukkit.getPluginManager().callEvent(blockCookEvent);

                    if (blockCookEvent.isCancelled()) {
                        return;
                    }

                    result = blockCookEvent.getResult();
                    itemstack1 = CraftItemStack.asNMSCopy(result);

                    Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), itemstack1);
                    entity.getItems().set(i, ItemStack.EMPTY);
                    level.sendBlockUpdated(pos, state, state, 3);
                }
            }
        }

        if (flag) {
            setChanged(level, pos, state);
        }

    }
}
