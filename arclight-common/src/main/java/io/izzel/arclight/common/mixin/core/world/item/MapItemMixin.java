package io.izzel.arclight.common.mixin.core.world.item;

import io.izzel.arclight.common.bridge.core.world.storage.MapDataBridge;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.bukkit.Bukkit;
import org.bukkit.event.server.MapInitializeEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import javax.annotation.Nullable;

@Mixin(MapItem.class)
public abstract class MapItemMixin {

    // @formatter:off
    @Shadow public static String makeKey(int p_42849_) { return null; }
    // @formatter:on

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public static int createNewSavedData(Level p_151121_, int p_151122_, int p_151123_, int p_151124_, boolean p_151125_, boolean p_151126_, ResourceKey<Level> p_151127_) {
        MapItemSavedData mapitemsaveddata = MapItemSavedData.createFresh((double) p_151122_, (double) p_151123_, (byte) p_151124_, p_151125_, p_151126_, p_151127_);
        MapInitializeEvent event = new MapInitializeEvent(((MapDataBridge) mapitemsaveddata).bridge$getMapView());
        Bukkit.getPluginManager().callEvent(event);
        int i = p_151121_.getFreeMapId();
        p_151121_.setMapData(makeKey(i), mapitemsaveddata);
        return i;
    }

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
