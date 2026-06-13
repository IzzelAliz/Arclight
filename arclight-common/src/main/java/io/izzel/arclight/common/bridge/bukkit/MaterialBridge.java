package io.izzel.arclight.common.bridge.bukkit;

import io.izzel.arclight.i18n.conf.MaterialPropertySpec;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.FuelValues;
import org.bukkit.block.BlockState;
import org.bukkit.craftbukkit.block.CraftBlock;
import org.bukkit.craftbukkit.inventory.CraftMetaItem;
import org.bukkit.inventory.meta.ItemMeta;

import javax.annotation.Nullable;
import java.util.function.Function;

public interface MaterialBridge {

    Identifier AIR = Identifier.parse("air");

    void bridge$setupBlock(Identifier key, MaterialPropertySpec spec);

    void bridge$setupVanillaBlock(MaterialPropertySpec spec);

    void bridge$setupItem(Identifier key, MaterialPropertySpec spec);

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

    default Item bridge$getCraftRemainingItem(Item item) {
        var remainder = item.getCraftingRemainder();
        return remainder == null ? null : remainder.item().value();
    }

    default int bridge$forge$getMaxStackSize(Item item) {
        return item.getDefaultMaxStackSize();
    }

    default int bridge$forge$getDurability(Item item) {
        return item.components().getOrDefault(DataComponents.MAX_DAMAGE, 0);
    }

    default int bridge$forge$getBurnTime(Item item) {
        return FuelValues.vanillaBurnTimes(RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY), FeatureFlags.DEFAULT_FLAGS)
            .burnDuration(new ItemStack(item));
    }
}
