package io.izzel.arclight.common.mixin.core.command.impl;

import com.mojang.brigadier.context.CommandContext;
import net.minecraft.command.CommandSource;
import net.minecraft.command.impl.GameRuleCommand;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.GameRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(GameRuleCommand.class)
public class GameRuleCommandMixin {

    @Redirect(method = "func_223485_b", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;getGameRules()Lnet/minecraft/world/GameRules;"))
    private static GameRules arclight$perWorldGameRule(MinecraftServer minecraftServer, CommandContext<CommandSource> context) {
        return context.getSource().getWorld().getGameRules();
    }

    @Redirect(method = "func_223486_b", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;getGameRules()Lnet/minecraft/world/GameRules;"))
    private static GameRules arclight$perWorldGameRule2(MinecraftServer minecraftServer, CommandSource source) {
        return source.getWorld().getGameRules();
    }
}
