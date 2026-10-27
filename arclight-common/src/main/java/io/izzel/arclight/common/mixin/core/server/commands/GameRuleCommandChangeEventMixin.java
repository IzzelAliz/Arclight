package io.izzel.arclight.common.mixin.core.server.commands;

import com.llamalad7.mixinextras.injector.WrapWithCondition;
import com.mojang.brigadier.context.CommandContext;
import io.izzel.arclight.common.bridge.core.commands.CommandSourceStackBridge;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import io.izzel.arclight.mixin.Local;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.commands.GameRuleCommand;
import net.minecraft.world.level.gamerules.GameRule;
import net.minecraft.world.level.gamerules.GameRules;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(GameRuleCommand.class)
public abstract class GameRuleCommandChangeEventMixin {

    @Decorate(method = "setRule(Lcom/mojang/brigadier/context/CommandContext;Lnet/minecraft/world/level/gamerules/GameRule;)I",
        at = @At(value = "INVOKE", target = "Lcom/mojang/brigadier/context/CommandContext;getArgument(Ljava/lang/String;Ljava/lang/Class;)Ljava/lang/Object;"),
        require = 1, allow = 1)
    private static <T> T arclight$worldGameRuleChange(CommandContext<CommandSourceStack> context, String name, Class<T> valueClass,
                                                   @Local(ordinal = 0) GameRule<T> rule) throws Throwable {
        T value = (T) DecorationOps.callsite().invoke(context, name, valueClass);
        CommandSourceStack source = context.getSource();
        // The helper constructs PaperWorldGameRuleChangeEvent (including value validation),
        // commits through the existing per-world bridge, and returns the old value on veto.
        // Returning that value here also updates native success feedback and command result.
        return CraftEventFactory.handleGameRuleSet(rule, value, source.getLevel(),
            ((CommandSourceStackBridge) source).bridge$getBukkitSender()).value();
    }

    @WrapWithCondition(method = "setRule(Lcom/mojang/brigadier/context/CommandContext;Lnet/minecraft/world/level/gamerules/GameRule;)I",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/gamerules/GameRules;set(Lnet/minecraft/world/level/gamerules/GameRule;Ljava/lang/Object;Lnet/minecraft/server/MinecraftServer;)V"),
        require = 1, allow = 1)
    private static <T> boolean arclight$alreadyAppliedWorldGameRule(GameRules rules, GameRule<T> rule, T value, MinecraftServer server) {
        // Paper replaces this global setter with handleGameRuleSet above. Calling both
        // would write on veto and send the global notifications a second time on success.
        return false;
    }
}
