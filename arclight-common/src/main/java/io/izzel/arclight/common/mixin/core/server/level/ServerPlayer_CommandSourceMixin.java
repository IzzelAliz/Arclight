package io.izzel.arclight.common.mixin.core.server.level;

import io.izzel.arclight.common.bridge.core.command.CommandSourceBridge;
import io.izzel.arclight.common.bridge.core.server.level.ServerPlayerBridge;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import org.bukkit.command.CommandSender;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Maps the vanilla player's delegated command source to its real Bukkit sender. */
@Mixin(targets = "net.minecraft.server.level.ServerPlayer$3")
public abstract class ServerPlayer_CommandSourceMixin implements CommandSourceBridge {
    @Unique private ServerPlayer arclight$owner;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void arclight$captureOwner(ServerPlayer owner, CallbackInfo ci) {
        this.arclight$owner = owner;
    }

    @Override
    public CommandSender bridge$getBukkitSender(CommandSourceStack wrapper) {
        return ((ServerPlayerBridge) this.arclight$owner).bridge$getBukkitEntity();
    }
}
