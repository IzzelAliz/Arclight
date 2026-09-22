package io.izzel.arclight.common.mixin.core.world.level.saveddata.maps;

import io.izzel.arclight.common.bridge.core.world.level.saveddata.maps.MapItemSavedDataBridge;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v.CraftServer;
import org.bukkit.craftbukkit.v.CraftWorld;
import org.bukkit.craftbukkit.v.map.CraftMapView;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;

import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


import java.util.List;

import java.util.UUID;


@Mixin(MapItemSavedData.class)
public abstract class MapItemSavedDataMixin implements MapItemSavedDataBridge {

    // @formatter:off
    @Shadow public ResourceKey<Level> dimension;
    @Shadow @Final @Mutable public static Codec<MapItemSavedData> CODEC;
    @Shadow @Final private List<MapItemSavedData.HoldingPlayer> carriedBy;
    // @formatter:on

    public CraftMapView mapView;
    private CraftServer server;
    public UUID uniqueId;
    public MapId id;

    @Inject(method = "<init>(IIBZZZLnet/minecraft/resources/ResourceKey;)V", at = @At("RETURN"))
    public void arclight$init(int p_164768_, int p_164769_, byte p_164770_, boolean p_164771_, boolean p_164772_, boolean p_164773_, ResourceKey<Level> p_164774_, CallbackInfo ci) {
        this.mapView = new CraftMapView((MapItemSavedData) (Object) this);
        this.server = (CraftServer) Bukkit.getServer();
        this.bridge$updateUUID();
    }

    @Inject(method = "<clinit>", at = @At("RETURN"), require = 1)
    private static void arclight$codec(CallbackInfo ci) {
        // Keep the complete vanilla codec, extending its map with CraftBukkit's UUID fields.
        var vanilla = MapCodec.assumeMapUnsafe(CODEC);
        CODEC = RecordCodecBuilder.create(instance -> instance.group(
            vanilla.forGetter((MapItemSavedData data) -> data),
            Codec.LONG.optionalFieldOf("UUIDLeast", 0L).forGetter(data -> {
                UUID uuid = ((MapItemSavedDataBridge) data).bridge$updateUUID();
                return uuid == null ? 0L : uuid.getLeastSignificantBits();
            }),
            Codec.LONG.optionalFieldOf("UUIDMost", 0L).forGetter(data -> {
                UUID uuid = ((MapItemSavedDataBridge) data).bridge$updateUUID();
                return uuid == null ? 0L : uuid.getMostSignificantBits();
            })
        ).apply(instance, (data, least, most) -> {
            ((MapItemSavedDataBridge) data).bridge$resolveDimension(least, most);
            return data;
        }));
    }

    @Override
    public void bridge$resolveDimension(long least, long most) {
        // A loaded dimension wins even if the stored UUID points elsewhere (Spigot 1.21.11).
        for (org.bukkit.World world : this.server.getWorlds()) {
            if (((CraftWorld) world).getHandle().dimension() == this.dimension) {
                return;
            }
        }
        if (least != 0L && most != 0L) {
            CraftWorld world = (CraftWorld) Bukkit.getWorld(new UUID(most, least));
            if (world != null) {
                this.dimension = world.getHandle().dimension();
                this.uniqueId = world.getUID();
                return;
            }
        }
        throw new IllegalArgumentException("Invalid map dimension: " + this.dimension);
    }

    @Override
    public UUID bridge$updateUUID() {
        if (this.uniqueId == null) {
            for (org.bukkit.World world : this.server.getWorlds()) {
                CraftWorld cWorld = (CraftWorld) world;
                if (cWorld.getHandle().dimension() != this.dimension) continue;
                this.uniqueId = cWorld.getUID();
                break;
            }
        }
        return this.uniqueId;
    }

    @Override
    public void bridge$setId(MapId id) {
        this.id = id;
    }

    @Override
    public List<MapItemSavedData.HoldingPlayer> bridge$getCarriedBy() {
        return this.carriedBy;
    }

    @Override
    public CraftMapView bridge$getMapView() {
        return mapView;
    }
}
