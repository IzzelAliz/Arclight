package io.izzel.arclight.common.mixin.core.world.storage;

import io.izzel.arclight.common.bridge.entity.player.ServerPlayerEntityBridge;
import io.izzel.arclight.common.bridge.world.storage.SaveHandlerBridge;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.storage.SaveHandler;
import org.apache.logging.log4j.Logger;
import org.bukkit.craftbukkit.v.entity.CraftPlayer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.UUID;

@Mixin(SaveHandler.class)
public class SaveHandlerMixin implements SaveHandlerBridge {

    // @formatter:off
    @Shadow(aliases = {"field_215773_b"}, remap = false) @Final private static Logger LOGGER;
    @Shadow @Final private File playersDirectory;
    @Shadow @Final private File worldDirectory;
    // @formatter:on

    private final Int2ObjectMap<UUID> uuidMap = new Int2ObjectOpenHashMap<>();

    @Inject(method = "readPlayerData", at = @At(value = "INVOKE", target = "Lnet/minecraft/nbt/CompoundNBT;contains(Ljava/lang/String;I)Z"))
    public void arclight$lastSeenTime(PlayerEntity player, CallbackInfoReturnable<CompoundNBT> cir) {
        if (player instanceof ServerPlayerEntity) {
            CraftPlayer craftPlayer = ((ServerPlayerEntityBridge) player).bridge$getBukkitEntity();
            // Only update first played if it is older than the one we have
            long modified = new File(this.playersDirectory, player.getUniqueID().toString() + ".dat").lastModified();
            if (modified < craftPlayer.getFirstPlayed()) {
                craftPlayer.setFirstPlayed(modified);
            }
        }
    }

    public String[] getSeenPlayers() {
        String[] arr = this.playersDirectory.list();

        if (arr == null) {
            arr = new String[0];
        }

        for (int i = 0; i < arr.length; ++i) {
            if (arr[i].endsWith(".dat")) {
                arr[i] = arr[i].substring(0, arr[i].length() - 4);
            }
        }

        return arr;
    }

    public UUID getUUID() {
        return getUUID(null);
    }

    public UUID getUUID(ServerWorld world) {
        int dimId = world == null ? 0 : world.dimension.getType().getId();
        UUID uuid = uuidMap.get(dimId);
        if (uuid != null) return uuid;
        File folder = world == null ? this.worldDirectory : world.dimension.getType().getDirectory(this.worldDirectory);
        File file1 = new File(folder, "uid.dat");
        if (file1.exists()) {
            try (DataInputStream dis = new DataInputStream(new FileInputStream(file1))) {
                uuid = new UUID(dis.readLong(), dis.readLong());
                uuidMap.put(dimId, uuid);
                return uuid;
            } catch (IOException ex) {
                LOGGER.warn("Failed to read " + file1 + ", generating new random UUID", ex);
            }
        }
        uuid = UUID.randomUUID();
        try (DataOutputStream dos = new DataOutputStream(new FileOutputStream(file1))) {
            dos.writeLong(uuid.getMostSignificantBits());
            dos.writeLong(uuid.getLeastSignificantBits());
        } catch (IOException ex) {
            LOGGER.warn("Failed to write " + file1, ex);
        }
        uuidMap.put(dimId, uuid);
        return uuid;
    }

    public File getPlayerDir() {
        return this.playersDirectory;
    }

    public CompoundNBT getPlayerData(String uuid) {
        try {
            final File file1 = new File(this.playersDirectory, uuid + ".dat");
            if (file1.exists()) {
                return CompressedStreamTools.readCompressed(new FileInputStream(file1));
            }
        } catch (Exception exception) {
            LOGGER.warn("Failed to load player data for " + uuid);
        }
        return null;
    }

    @Override
    public String[] bridge$getSeenPlayers() {
        return getSeenPlayers();
    }

    @Override
    public UUID bridge$getUUID(ServerWorld world) {
        return getUUID(world);
    }

    @Override
    public File bridge$getPlayerDir() {
        return getPlayerDir();
    }

    @Override
    public CompoundNBT bridge$getPlayerData(String uuid) {
        return getPlayerData(uuid);
    }
}
