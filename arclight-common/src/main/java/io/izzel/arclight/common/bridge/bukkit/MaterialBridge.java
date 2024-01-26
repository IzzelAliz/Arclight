package io.izzel.arclight.common.bridge.bukkit;

import io.izzel.arclight.i18n.conf.MaterialPropertySpec;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import org.bukkit.block.BlockState;
import org.bukkit.craftbukkit.v.block.CraftBlock;
import org.bukkit.craftbukkit.v.inventory.CraftMetaItem;
import org.bukkit.inventory.meta.ItemMeta;

import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import java.util.function.Function;

public interface MaterialBridge {

    void bridge$setupBlock(ResourceLocation key, Block block, MaterialPropertySpec spec);

    void bridge$setupVanillaBlock(MaterialPropertySpec spec);

    void bridge$setupItem(ResourceLocation key, Item item, MaterialPropertySpec spec);

    void bridge$setBlock();

    void bridge$setItem();

    @Nullable
    MaterialPropertySpec bridge$getSpec();

    MaterialPropertySpec.MaterialType bridge$getType();

    Function<CraftMetaItem, ItemMeta> bridge$itemMetaFactory();

    void bridge$setItemMetaFactory(Function<CraftMetaItem, ItemMeta> func);

    Function<CraftBlock, BlockState> bridge$blockStateFactory();

    void bridge$setBlockStateFactory(Function<CraftBlock, BlockState> func);

    boolean bridge$shouldApplyStateFactory();

    default Item bridge$getCraftRemainingItem(Item item)  {
        return item.getCraftingRemainingItem();
    }

    default int bridge$forge$getMaxStackSize(Item item) {
        return item.getMaxStackSize();
    }

    default int bridge$forge$getDurability(Item item) {
        return item.getMaxDamage();
    }

    default int bridge$forge$getBurnTime(Item item)  {
        var result = AbstractFurnaceBlockEntity.getFuel().get(item);
        return result != null ? result : 0;
    }
}
