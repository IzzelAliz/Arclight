package io.izzel.arclight.common.mixin.core.item;

import io.izzel.arclight.common.bridge.world.storage.MapDataBridge;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.bukkit.Bukkit;
import org.bukkit.event.server.MapInitializeEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(MapItem.class)
public abstract class FilledMapItemMixin {

    // @formatter:off
    @Shadow private static MapItemSavedData createAndStoreSavedData(ItemStack stack, Level worldIn, int x, int z, int scale, boolean trackingPosition, boolean unlimitedTracking, ResourceKey<Level> dimensionTypeIn) { return null; }
    // @formatter:on

    @Inject(method = "createAndStoreSavedData", locals = LocalCapture.CAPTURE_FAILHARD, at = @At("RETURN"))
    private static void arclight$mapInit(ItemStack stack, Level worldIn, int x, int z, int scale, boolean trackingPosition,
                                         boolean unlimitedTracking, ResourceKey<Level> dimensionTypeIn, CallbackInfoReturnable<MapItemSavedData> cir,
                                         int i, MapItemSavedData mapData) {
        MapInitializeEvent event = new MapInitializeEvent(((MapDataBridge) mapData).bridge$getMapView());
        Bukkit.getPluginManager().callEvent(event);
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public static int getMapId(ItemStack stack) {
        CompoundTag compoundnbt = stack.getTag();
        return compoundnbt != null && compoundnbt.contains("map", 99) ? compoundnbt.getInt("map") : -1;
    }

    @Inject(method = "getOrCreateSavedData", cancellable = true, at = @At("HEAD"))
    private static void arclight$nonFilledMap(ItemStack stack, Level worldIn, CallbackInfoReturnable<MapItemSavedData> cir) {
        if (stack != null && worldIn instanceof ServerLevel && stack.getItem() == Items.MAP) {
            MapItemSavedData mapdata = MapItem.getSavedData(stack, worldIn);
            if (mapdata == null) {
                mapdata = createAndStoreSavedData(stack, worldIn, worldIn.getLevelData().getXSpawn(), worldIn.getLevelData().getZSpawn(), 3, false, false, worldIn.dimension());
            }
            cir.setReturnValue(mapdata);
        }
    }
}
