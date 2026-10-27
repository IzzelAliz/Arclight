package io.izzel.arclight.common.mod.compat;

import com.mojang.brigadier.tree.CommandNode;
import io.izzel.arclight.api.Unsafe;
import io.izzel.arclight.common.bridge.core.commands.CommandSourceStackBridge;
import io.papermc.paper.command.brigadier.APICommandMeta;
import org.jspecify.annotations.Nullable;

import java.lang.reflect.Modifier;
import java.util.Map;

public class CommandNodeHooks {

    private static final long CHILDREN, LITERALS, ARGUMENTS, CURRENT;
    private static final long API_COMMAND_META, WRAPPED_CACHED, UNWRAPPED_CACHED;
    private static final Object CURRENT_BASE;

    static {
        try {
            CHILDREN = Unsafe.objectFieldOffset(CommandNode.class.getDeclaredField("children"));
            LITERALS = Unsafe.objectFieldOffset(CommandNode.class.getDeclaredField("literals"));
            ARGUMENTS = Unsafe.objectFieldOffset(CommandNode.class.getDeclaredField("arguments"));
            CURRENT_BASE = Unsafe.staticFieldBase(CommandNode.class.getDeclaredField("CURRENT_COMMAND"));
            CURRENT = Unsafe.staticFieldOffset(CommandNode.class.getDeclaredField("CURRENT_COMMAND"));
            API_COMMAND_META = stateFieldOffset("arclight$apiCommandMeta");
            WRAPPED_CACHED = stateFieldOffset("arclight$wrappedCached");
            UNWRAPPED_CACHED = stateFieldOffset("arclight$unwrappedCached");
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    private static long stateFieldOffset(String name) throws NoSuchFieldException {
        var field = CommandNode.class.getDeclaredField(name);
        if (field.getType() != Object.class || (field.getModifiers() & (Modifier.STATIC | Modifier.FINAL)) != 0) {
            throw new IllegalStateException("Conflicting Brigadier state field: " + name);
        }
        return Unsafe.objectFieldOffset(field);
    }

    @SuppressWarnings("unchecked")
    public static void removeCommand(CommandNode<?> node, String command) {
        ((Map<String, ?>) Unsafe.getObject(node, CHILDREN)).remove(command);
        ((Map<String, ?>) Unsafe.getObject(node, LITERALS)).remove(command);
        ((Map<String, ?>) Unsafe.getObject(node, ARGUMENTS)).remove(command);
    }

    public static CommandNode<?> getCurrent() {
        return (CommandNode<?>) Unsafe.getObjectVolatile(CURRENT_BASE, CURRENT);
    }

    public static @Nullable APICommandMeta getApiCommandMeta(CommandNode<?> node) {
        return (APICommandMeta) Unsafe.getObject(node, API_COMMAND_META);
    }

    public static void setApiCommandMeta(CommandNode<?> node, @Nullable APICommandMeta meta) {
        Unsafe.putObject(node, API_COMMAND_META, meta);
    }

    @SuppressWarnings("unchecked")
    public static <S> @Nullable CommandNode<S> getWrappedCached(CommandNode<?> node) {
        return (CommandNode<S>) Unsafe.getObject(node, WRAPPED_CACHED);
    }

    public static void setWrappedCached(CommandNode<?> node, @Nullable CommandNode<?> wrapped) {
        Unsafe.putObject(node, WRAPPED_CACHED, wrapped);
    }

    @SuppressWarnings("unchecked")
    public static <S> @Nullable CommandNode<S> getUnwrappedCached(CommandNode<?> node) {
        return (CommandNode<S>) Unsafe.getObject(node, UNWRAPPED_CACHED);
    }

    public static void setUnwrappedCached(CommandNode<?> node, @Nullable CommandNode<?> unwrapped) {
        Unsafe.putObject(node, UNWRAPPED_CACHED, unwrapped);
    }

    public static <S> boolean canUse(CommandNode<S> node, S source) {
        if (source instanceof CommandSourceStackBridge s) {
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
