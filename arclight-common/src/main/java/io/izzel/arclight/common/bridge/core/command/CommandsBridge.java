package io.izzel.arclight.common.bridge.core.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.tree.CommandNode;

import java.util.Map;
import java.util.function.Function;

public interface CommandsBridge {
    default <S, T> void bridge$forge$mergeNode(CommandNode<S> sourceNode, CommandNode<T> resultNode,
                                       Map<CommandNode<S>, CommandNode<T>> sourceToResult,
                                       S canUse, Command<T> execute,
                                       Function<SuggestionProvider<S>, SuggestionProvider<T>> sourceToResultSuggestion) {
    }
}
