package io.izzel.arclight.common.mixin.core.world.storage;

import io.izzel.arclight.common.bridge.world.storage.MapDataBridge;
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

import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;

@Mixin(MapItemSavedData.class)
public abstract class MapDataMixin implements MapDataBridge {

    // @formatter:off
    @Shadow public ResourceKey<Level> dimension;
    // @formatter:on

    public CraftMapView mapView;
    private CraftServer server;
    private UUID uniqueId;

    @Inject(method = "<init>", at = @At("RETURN"))
    public void arclight$init(String mapname, CallbackInfo ci) {
        this.mapView = new CraftMapView((MapItemSavedData) (Object) this);
        this.server = (CraftServer) Bukkit.getServer();
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    @Redirect(method = "load", at = @At(value = "INVOKE", target = "Ljava/util/Optional;orElseThrow(Ljava/util/function/Supplier;)Ljava/lang/Object;"))
    public Object arclight$customDimension(Optional<ResourceKey<Level>> optional, Supplier<?> exceptionSupplier, CompoundTag nbt) {
        return optional.orElseGet(() -> {
            long least = nbt.getLong("UUIDLeast");
            long most = nbt.getLong("UUIDMost");
            if (least != 0L && most != 0L) {
                this.uniqueId = new UUID(most, least);
                CraftWorld world = (CraftWorld) this.server.getWorld(this.uniqueId);
                if (world != null) {
                    return world.getHandle().dimension();
                }
            }
            throw new IllegalArgumentException("Invalid map dimension: " + nbt.get("dimension"));
        });
    }

    @Inject(method = "save", at = @At("HEAD"))
    public void arclight$storeDimension(CompoundTag compound, CallbackInfoReturnable<CompoundTag> cir) {
        if (this.uniqueId == null) {
            for (org.bukkit.World world : this.server.getWorlds()) {
                CraftWorld cWorld = (CraftWorld) world;
                if (cWorld.getHandle().dimension() != this.dimension) continue;
                this.uniqueId = cWorld.getUID();
                break;
            }
        }
        if (this.uniqueId != null) {
            compound.putLong("UUIDLeast", this.uniqueId.getLeastSignificantBits());
            compound.putLong("UUIDMost", this.uniqueId.getMostSignificantBits());
        }
    }

    @Override
    public CraftMapView bridge$getMapView() {
        return mapView;
    }
}
