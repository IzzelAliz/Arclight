package io.izzel.arclight.neoforge.mixin.core.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.tree.CommandNode;
import io.izzel.arclight.common.bridge.core.command.CommandsBridge;
import net.minecraft.commands.Commands;
import net.neoforged.neoforge.server.command.CommandHelper;
import org.spongepowered.asm.mixin.Mixin;

import java.util.Map;
import java.util.function.Function;

@Mixin(Commands.class)
public abstract class CommandsMixin_NeoForge implements CommandsBridge {
    @Override
    public <S, T> void bridge$forge$mergeNode(CommandNode<S> sourceNode, CommandNode<T> resultNode,
                                              Map<CommandNode<S>, CommandNode<T>> sourceToResult,
                                              S canUse, Command<T> execute,
                                              Function<SuggestionProvider<S>, SuggestionProvider<T>> sourceToResultSuggestion) {
        CommandHelper.mergeCommandNode(sourceNode, resultNode, sourceToResult, canUse, execute, sourceToResultSuggestion);
    }
}
