package io.izzel.arclight.common.mixin.core.server.network.config;

import io.izzel.arclight.common.bridge.core.server.players.PlayerListBridge;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import io.izzel.arclight.mixin.Local;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.NameAndId;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Optional;

@Mixin(targets = "net.minecraft.server.network.config.PrepareSpawnTask$Ready")
public abstract class PrepareSpawnTask_ReadyMixin {

    @Decorate(method = "spawn", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/players/PlayerList;loadPlayerData(Lnet/minecraft/server/players/NameAndId;)Ljava/util/Optional;"))
    private Optional<CompoundTag> arclight$preparePlayerData(PlayerList list, NameAndId profile,
            @Local(ordinal = 0) Connection connection, @Local(ordinal = -1) ServerPlayer player) throws Throwable {
        var result = (Optional<CompoundTag>) DecorationOps.callsite().invoke(list, profile);
        ((PlayerListBridge) list).bridge$preparePlayerData(connection, player, result);
        return result;
    }
}
