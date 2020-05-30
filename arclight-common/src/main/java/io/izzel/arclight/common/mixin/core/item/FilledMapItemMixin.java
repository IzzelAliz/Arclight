package io.izzel.arclight.common.mixin.core.item;

import net.minecraft.item.FilledMapItem;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.storage.MapData;
import org.bukkit.Bukkit;
import org.bukkit.event.server.MapInitializeEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;
import io.izzel.arclight.common.bridge.world.storage.MapDataBridge;

@Mixin(FilledMapItem.class)
public class FilledMapItemMixin {

    @Inject(method = "createMapData", locals = LocalCapture.CAPTURE_FAILHARD, at = @At("RETURN"))
    private static void arclight$mapInit(ItemStack stack, World worldIn, int x, int z, int scale, boolean trackingPosition,
                                         boolean unlimitedTracking, DimensionType dimensionTypeIn, CallbackInfoReturnable<MapData> cir,
                                         int i, MapData mapData) {
        MapInitializeEvent event = new MapInitializeEvent(((MapDataBridge) mapData).bridge$getMapView());
        Bukkit.getPluginManager().callEvent(event);
    }
}
