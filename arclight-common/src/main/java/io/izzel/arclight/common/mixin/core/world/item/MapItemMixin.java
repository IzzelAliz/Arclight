package io.izzel.arclight.common.mixin.core.world.item;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MapItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import javax.annotation.Nullable;

@Mixin(MapItem.class)
public abstract class MapItemMixin {

    /**
     * @author IzzelAliz
     * @reason
     */
    @Nullable
    @Overwrite
    public static Integer getMapId(ItemStack stack) {
        CompoundTag compoundnbt = stack.getTag();
        return compoundnbt != null && compoundnbt.contains("map", 99) ? compoundnbt.getInt("map") : -1;
    }
}
