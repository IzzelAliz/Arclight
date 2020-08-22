package io.izzel.arclight.common.mixin.core.item;

import io.izzel.arclight.common.bridge.world.storage.MapDataBridge;
import net.minecraft.item.FilledMapItem;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.RegistryKey;
import net.minecraft.world.World;
import net.minecraft.world.storage.MapData;
import org.bukkit.Bukkit;
import org.bukkit.event.server.MapInitializeEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(FilledMapItem.class)
public class FilledMapItemMixin {

    @Inject(method = "createMapData", locals = LocalCapture.CAPTURE_FAILHARD, at = @At("RETURN"))
    private static void arclight$mapInit(ItemStack stack, World worldIn, int x, int z, int scale, boolean trackingPosition,
                                         boolean unlimitedTracking, RegistryKey<World> dimensionTypeIn, CallbackInfoReturnable<MapData> cir,
                                         int i, MapData mapData) {
        MapInitializeEvent event = new MapInitializeEvent(((MapDataBridge) mapData).bridge$getMapView());
        Bukkit.getPluginManager().callEvent(event);
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public static int getMapId(ItemStack stack) {
        CompoundNBT compoundnbt = stack.getTag();
        return compoundnbt != null && compoundnbt.contains("map", 99) ? compoundnbt.getInt("map") : -1;
    }
}
