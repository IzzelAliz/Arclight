package io.izzel.arclight.common.mixin.core.world.storage;

import io.izzel.arclight.common.bridge.entity.player.ServerPlayerEntityBridge;
import io.izzel.arclight.common.bridge.world.storage.PlayerDataBridge;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.world.storage.PlayerData;
import org.apache.logging.log4j.Logger;
import org.bukkit.craftbukkit.v.entity.CraftPlayer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.File;
import java.io.FileInputStream;

@Mixin(PlayerData.class)
public class PlayerDataMixin implements PlayerDataBridge {

    // @formatter:off
    @Shadow @Final private File playerDataFolder;
    @Shadow @Final private static Logger LOGGER;
    // @formatter:on

    @Inject(method = "loadPlayerData", at = @At(value = "INVOKE", target = "Lnet/minecraft/nbt/CompoundNBT;contains(Ljava/lang/String;I)Z"))
    private void arclight$lastSeenTime(PlayerEntity player, CallbackInfoReturnable<CompoundNBT> cir) {
        if (player instanceof ServerPlayerEntity) {
            CraftPlayer craftPlayer = ((ServerPlayerEntityBridge) player).bridge$getBukkitEntity();
            // Only update first played if it is older than the one we have
            long modified = new File(this.playerDataFolder, player.getUniqueID().toString() + ".dat").lastModified();
            if (modified < craftPlayer.getFirstPlayed()) {
                craftPlayer.setFirstPlayed(modified);
            }
        }
    }

    public File getPlayerDir() {
        return this.playerDataFolder;
    }

    public CompoundNBT getPlayerData(String uuid) {
        try {
            final File file1 = new File(this.playerDataFolder, uuid + ".dat");
            if (file1.exists()) {
                return CompressedStreamTools.readCompressed(new FileInputStream(file1));
            }
        } catch (Exception exception) {
            LOGGER.warn("Failed to load player data for " + uuid);
        }
        return null;
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
