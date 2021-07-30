package io.izzel.arclight.common.mod.compat;

import com.mojang.brigadier.tree.CommandNode;
import io.izzel.arclight.api.Unsafe;
import io.izzel.arclight.common.bridge.core.command.CommandSourceBridge;

import java.util.Map;

public class CommandNodeHooks {

    private static final long CHILDREN, LITERALS, ARGUMENTS;

    static {
        try {
            CHILDREN = Unsafe.objectFieldOffset(CommandNode.class.getDeclaredField("children"));
            LITERALS = Unsafe.objectFieldOffset(CommandNode.class.getDeclaredField("literals"));
            ARGUMENTS = Unsafe.objectFieldOffset(CommandNode.class.getDeclaredField("arguments"));
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    @SuppressWarnings("unchecked")
    public static void removeCommand(CommandNode<?> node, String command) {
        ((Map<String, ?>) Unsafe.getObject(node, CHILDREN)).remove(command);
        ((Map<String, ?>) Unsafe.getObject(node, LITERALS)).remove(command);
        ((Map<String, ?>) Unsafe.getObject(node, ARGUMENTS)).remove(command);
    }

    public static <S> boolean canUse(CommandNode<S> node, S source) {
        if (source instanceof CommandSourceBridge s) {
            try {
                s.bridge$setCurrentCommand(node);
                return node.canUse(source);
            } finally {
                s.bridge$setCurrentCommand(null);
            }
        } else {
            return node.canUse(source);
        }
    }
}
