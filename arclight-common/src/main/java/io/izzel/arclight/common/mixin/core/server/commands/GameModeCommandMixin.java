package io.izzel.arclight.common.mixin.core.server.commands;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import io.izzel.arclight.common.bridge.core.server.level.ServerPlayerGameModeBridge;
import io.izzel.arclight.common.mod.util.ArclightCaptures;
import io.papermc.paper.adventure.PaperAdventure;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.commands.GameModeCommand;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(GameModeCommand.class)
public class GameModeCommandMixin {

    @WrapOperation(method = "setGameMode(Lnet/minecraft/commands/CommandSourceStack;Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/world/level/GameType;)Z",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;setGameMode(Lnet/minecraft/world/level/GameType;)Z"),
        require = 1, expect = 1, allow = 1)
    private static boolean arclight$commandGameModeChange(ServerPlayer player, GameType type, Operation<Boolean> original,
                                                          @Local(argsOnly = true) CommandSourceStack source) {
        boolean[] changed = new boolean[1];
        PlayerGameModeChangeEvent event = ((ServerPlayerGameModeBridge) player.gameMode).bridge$withGameModeChange(
            ArclightCaptures.takeGameModeCause(player), null, () -> changed[0] = original.call(player, type));
        // Cancellation feedback belongs only to this command/switcher caller, not the plugin setter.
        if (!changed[0] && event != null && event.cancelMessage() != null) {
            source.sendSuccess(() -> PaperAdventure.asVanilla(event.cancelMessage()), true);
        }
        // Preserve native/mod vetoes and let the original caller log/count only real success.
        return changed[0];
    }
}
