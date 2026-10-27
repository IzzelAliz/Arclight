package io.izzel.arclight.common.mixin.core.server.network;

import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import io.izzel.arclight.common.mod.util.ArclightCaptures;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.level.GameType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ServerGamePacketListenerImpl.class)
public class ServerGameModeSwitcherMixin {

    @Decorate(method = "handleChangeGameMode(Lnet/minecraft/network/protocol/game/ServerboundChangeGameModePacket;)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/server/commands/GameModeCommand;setGameMode(Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/world/level/GameType;)V"),
        require = 1, expect = 1, allow = 1)
    private void arclight$switcherGameModeChange(ServerPlayer player, GameType type) throws Throwable {
        // Keep the native permission gate, void command entry and original arguments intact.
        var previous = ArclightCaptures.gameModeSwitcher;
        ArclightCaptures.gameModeSwitcher = player;
        try {
            DecorationOps.callsite().invoke(player, type);
        } finally {
            ArclightCaptures.gameModeSwitcher = previous;
        }
    }
}
