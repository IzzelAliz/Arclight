package io.izzel.arclight.common.mixin.core.server.commands;

import io.izzel.arclight.common.bridge.core.entity.player.ServerPlayerEntityBridge;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.server.commands.SetSpawnCommand;
import net.minecraft.server.level.ServerPlayer;
import org.bukkit.event.player.PlayerSpawnChangeEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collection;

@Mixin(SetSpawnCommand.class)
public class SetSpawnCommandMixin {

    @Inject(method = "setSpawn", at = @At("HEAD"))
    private static void arclight$cause(CommandSourceStack p_138650_, Collection<ServerPlayer> players, BlockPos p_138652_, float p_138653_, CallbackInfoReturnable<Integer> cir) {
        for (ServerPlayer player : players) {
            ((ServerPlayerEntityBridge) player).bridge$pushChangeSpawnCause(PlayerSpawnChangeEvent.Cause.COMMAND);
        }
    }
}
