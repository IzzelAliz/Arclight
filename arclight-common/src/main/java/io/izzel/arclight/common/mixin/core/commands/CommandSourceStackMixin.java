package io.izzel.arclight.common.mixin.core.commands;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.tree.CommandNode;
import io.izzel.arclight.common.bridge.core.command.CommandSourceBridge;
import io.izzel.arclight.common.bridge.core.command.ICommandSourceBridge;
import io.izzel.arclight.common.bridge.core.entity.player.ServerPlayerEntityBridge;
import io.izzel.arclight.common.mod.compat.CommandNodeHooks;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraftforge.common.util.FakePlayer;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.v.CraftServer;
import org.bukkit.craftbukkit.v.command.VanillaCommandWrapper;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CommandSourceStack.class)
public abstract class CommandSourceStackMixin implements CommandSourceBridge {

    // @formatter:off
    @Shadow @Final public CommandSource source;
    @Shadow public abstract ServerLevel getLevel();
    @Shadow @Final private int permissionLevel;
    // @formatter:on

    public CommandNode currentCommand;

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Inject(method = "hasPermission", cancellable = true, at = @At("HEAD"))
    public void arclight$checkPermission(int level, CallbackInfoReturnable<Boolean> cir) {
        CommandNode currentCommand = bridge$getCurrentCommand();
        if (currentCommand != null) {
            cir.setReturnValue(hasPermission(level, VanillaCommandWrapper.getPermission(currentCommand)));
        }
    }

    @Redirect(method = "broadcastToAdmins", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/players/PlayerList;isOp(Lcom/mojang/authlib/GameProfile;)Z"))
    private boolean arclight$feedbackPermission(PlayerList instance, GameProfile profile) {
        return ((ServerPlayerEntityBridge) instance.getPlayer(profile.getId())).bridge$getBukkitEntity().hasPermission("minecraft.admin.command_feedback");
    }

    public boolean hasPermission(int i, String bukkitPermission) {
        // World is null when loading functions
        return ((getLevel() == null || !((CraftServer) Bukkit.getServer()).ignoreVanillaPermissions) && this.permissionLevel >= i) || getBukkitSender().hasPermission(bukkitPermission);
    }

    @Override
    public boolean bridge$hasPermission(int i, String bukkitPermission) {
        return hasPermission(i, bukkitPermission);
    }

    @Override
    public CommandNode<?> bridge$getCurrentCommand() {
        if (currentCommand == null) {
            return CommandNodeHooks.getCurrent();
        } else {
            return currentCommand;
        }
    }

    @Override
    public void bridge$setCurrentCommand(CommandNode<?> node) {
        this.currentCommand = node;
    }

    public CommandSender getBukkitSender() {
        return ((ICommandSourceBridge) this.source).bridge$getBukkitSender((CommandSourceStack) (Object) this);
    }

    @Override
    public CommandSender bridge$getBukkitSender() {
        return getBukkitSender();
    }
}
