package io.izzel.arclight.common.mixin.core.world.storage;

import io.izzel.arclight.common.bridge.world.storage.MapDataBridge;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.RegistryKey;
import net.minecraft.world.World;
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

import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

@Mixin(MapData.class)
public abstract class MapDataMixin implements MapDataBridge {

    // @formatter:off
    @Shadow public RegistryKey<World> dimension;
    // @formatter:on

    public CraftMapView mapView;
    private CraftServer server;
    private UUID uniqueId;

    @Inject(method = "<init>", at = @At("RETURN"))
    public void arclight$init(String mapname, CallbackInfo ci) {
        this.mapView = new CraftMapView((MapData) (Object) this);
        this.server = (CraftServer) Bukkit.getServer();
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    @Redirect(method = "read", at = @At(value = "INVOKE", target = "Ljava/util/Optional;orElseThrow(Ljava/util/function/Supplier;)Ljava/lang/Object;"))
    public Object arclight$customDimension(Optional<RegistryKey<World>> optional, Supplier<?> exceptionSupplier, CompoundNBT nbt) {
        return optional.orElseGet(() -> {
            long least = nbt.getLong("UUIDLeast");
            long most = nbt.getLong("UUIDMost");
            if (least != 0L && most != 0L) {
                this.uniqueId = new UUID(most, least);
                CraftWorld world = (CraftWorld) this.server.getWorld(this.uniqueId);
                if (world != null) {
                    return world.getHandle().getDimensionKey();
                }
            }
            throw new IllegalArgumentException("Invalid map dimension: " + nbt.get("dimension"));
        });
    }

    @Inject(method = "write", at = @At("HEAD"))
    public void arclight$storeDimension(CompoundNBT compound, CallbackInfoReturnable<CompoundNBT> cir) {
        if (this.uniqueId == null) {
            for (org.bukkit.World world : this.server.getWorlds()) {
                CraftWorld cWorld = (CraftWorld) world;
                if (cWorld.getHandle().getDimensionKey() != this.dimension) continue;
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
