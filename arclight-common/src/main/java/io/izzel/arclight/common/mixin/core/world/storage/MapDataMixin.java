package io.izzel.arclight.common.mixin.core.world.storage;

import io.izzel.arclight.common.bridge.world.dimension.DimensionTypeBridge;
import io.izzel.arclight.common.bridge.world.storage.MapDataBridge;
import io.izzel.arclight.common.mod.ArclightConstants;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.dimension.OverworldDimension;
import net.minecraft.world.storage.MapData;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v.CraftServer;
import org.bukkit.craftbukkit.v.CraftWorld;
import org.bukkit.craftbukkit.v.map.CraftMapView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

@Mixin(MapData.class)
public abstract class MapDataMixin implements MapDataBridge {

    // @formatter:off
    @Shadow public DimensionType dimension;
    // @formatter:on

    public CraftMapView mapView;
    private CraftServer server;
    private UUID uniqueId;

    @Inject(method = "<init>", at = @At("RETURN"))
    public void arclight$init(String mapname, CallbackInfo ci) {
        this.mapView = new CraftMapView((MapData) (Object) this);
        this.server = (CraftServer) Bukkit.getServer();
    }

    @Redirect(method = "read", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/dimension/DimensionType;getById(I)Lnet/minecraft/world/dimension/DimensionType;"))
    public DimensionType arclight$customDimension(int id, CompoundNBT nbt) {
        DimensionType type;
        long least = nbt.getLong("UUIDLeast");
        long most = nbt.getLong("UUIDMost");

        if (least != 0L && most != 0L) {
            this.uniqueId = new UUID(most, least);

            CraftWorld world = (CraftWorld) server.getWorld(this.uniqueId);
            // Check if the stored world details are correct.
            if (world == null) {
                type = DimensionType.getById(id);
                if (type == null) {
                    /* All Maps which do not have their valid world loaded are set to a dimension which hopefully won't be reached.
                       This is to prevent them being corrupted with the wrong map data. */
                    type = this.bridge$dimension(ArclightConstants.ARCLIGHT_DIMENSION, "", "", OverworldDimension::new, false);
                    ((DimensionTypeBridge) type).bridge$setType(DimensionType.OVERWORLD);

                }
            } else {
                type = world.getHandle().dimension.getType();
            }
        } else {
            type = DimensionType.getById(id);
        }
        return type;
    }

    @Inject(method = "write", at = @At("HEAD"))
    public void arclight$storeDimension(CompoundNBT compound, CallbackInfoReturnable<CompoundNBT> cir) {
        if (this.dimension.getId() >= CraftWorld.CUSTOM_DIMENSION_OFFSET) {
            if (this.uniqueId == null) {
                for (org.bukkit.World world : server.getWorlds()) {
                    CraftWorld cWorld = (CraftWorld) world;
                    if (cWorld.getHandle().dimension.getType() == this.dimension) {
                        this.uniqueId = cWorld.getUID();
                        break;
                    }
                }
            }
            /* Perform a second check to see if a matching world was found, this is a necessary
               change incase Maps are forcefully unlinked from a World and lack a UID.*/
            if (this.uniqueId != null) {
                compound.putLong("UUIDLeast", this.uniqueId.getLeastSignificantBits());
                compound.putLong("UUIDMost", this.uniqueId.getMostSignificantBits());
            }
        }
    }

    @Override
    public CraftMapView bridge$getMapView() {
        return mapView;
    }
}
