package io.izzel.arclight.common.mixin.core.command;

import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.RootCommandNode;
import io.izzel.arclight.common.bridge.command.CommandNodeBridge;
import io.izzel.arclight.common.bridge.entity.player.ServerPlayerEntityBridge;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.command.ISuggestionProvider;
import net.minecraft.entity.player.ServerPlayerEntity;
import org.bukkit.Bukkit;
import org.bukkit.event.player.PlayerCommandSendEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.LinkedHashSet;
import java.util.Map;

@Mixin(Commands.class)
public class CommandsMixin {

    @Inject(method = "send", locals = LocalCapture.CAPTURE_FAILHARD, at = @At(value = "INVOKE", shift = At.Shift.AFTER, target = "Lnet/minecraft/command/Commands;commandSourceNodesToSuggestionNodes(Lcom/mojang/brigadier/tree/CommandNode;Lcom/mojang/brigadier/tree/CommandNode;Lnet/minecraft/command/CommandSource;Ljava/util/Map;)V"))
    private void arclight$playerCommandSend(ServerPlayerEntity player, CallbackInfo ci, Map<CommandNode<CommandSource>, CommandNode<ISuggestionProvider>> map , RootCommandNode<ISuggestionProvider> node) {
        LinkedHashSet<String> set = new LinkedHashSet<>();
        for (CommandNode<ISuggestionProvider> child : node.getChildren()) {
            set.add(child.getName());
        }
        PlayerCommandSendEvent event = new PlayerCommandSendEvent(((ServerPlayerEntityBridge) player).bridge$getBukkitEntity(), new LinkedHashSet<>(set));
        Bukkit.getPluginManager().callEvent(event);
        for (String s : set) {
            if (!event.getCommands().contains(s)) {
                ((CommandNodeBridge) node).bridge$removeCommand(s);
            }
        }
    }
}
