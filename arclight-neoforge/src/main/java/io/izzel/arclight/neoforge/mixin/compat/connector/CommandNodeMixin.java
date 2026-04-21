package io.izzel.arclight.neoforge.mixin.compat.connector;

import com.mojang.brigadier.tree.ArgumentCommandNode;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.izzel.arclight.common.mod.mixins.annotation.LoadIfMod;
import io.izzel.arclight.common.mod.mixins.annotation.TransformAccess;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.*;

import java.util.Map;
import java.util.function.Predicate;

/*
 * Since Sinytra Connector moves Brigadier from lib layer to game layer, redefining in lib layer won't work
 * because it is already loaded in game layer when connector presents. We may use a mixin to fix this.
 * When connector is present it can be mixed into because it's in the game layer :)
 */
@LoadIfMod(modid = "connector", condition = LoadIfMod.ModCondition.PRESENT)
@Mixin(value = CommandNode.class, remap = false)
public class CommandNodeMixin<S> {

    // @formatter:off
    @Shadow @Final private Map<String, CommandNode<S>> children;
    @Shadow @Final private Map<String, LiteralCommandNode<S>> literals;
    @Shadow @Final private Map<String, ArgumentCommandNode<S, ?>> arguments;
    @Shadow @Final private Predicate<S> requirement;
    // @formatter:on

    /*
     * This is used when the current command is not present in CommandSourceStack.
     */
    @TransformAccess(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC)
    private static CommandNode<?> CURRENT_COMMAND;

    @Unique
    public void removeCommand(String name) {
        children.remove(name);
        literals.remove(name);
        arguments.remove(name);
    }

    /**
     * @author InitAuther97
     * @reason pre-cache CommandNode for requirement test.
     */
    @Overwrite
    public boolean canUse(final S source) {
        if (source instanceof final io.izzel.arclight.common.bridge.core.commands.CommandSourceStackBridge bridge) {
            try {
                bridge.bridge$setCurrentCommand((CommandNode<?>) (Object) this);
                return requirement.test(source);
            } finally {
                bridge.bridge$setCurrentCommand(null);
            }
        }
        // CraftBukkit end
        return requirement.test(source);
    }
}
