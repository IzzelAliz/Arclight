package io.izzel.arclight.mixin.bukkit;

import com.mojang.brigadier.tree.ArgumentCommandNode;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.izzel.arclight.bridge.command.CommandSourceBridge;
import net.minecraft.command.CommandSource;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;
import java.util.function.Predicate;

@Mixin(CommandNode.class)
public class CommandNodeMixin<S> {

    // @formatter:off
    @Shadow(remap = false) private Map<String, CommandNode<S>> children;
    @Shadow(remap = false) private Map<String, LiteralCommandNode<S>> literals;
    @Shadow(remap = false) private Map<String, ArgumentCommandNode<S, ?>> arguments;
    @Shadow(remap = false) @Final private Predicate<S> requirement;
    // @formatter:on

    public void removeCommand(String name) {
        children.remove(name);
        literals.remove(name);
        arguments.remove(name);
    }

    @Inject(method = "canUse", remap = false, cancellable = true, at = @At("HEAD"))
    public void on(S source, CallbackInfoReturnable<Boolean> cir) {
        if (source instanceof CommandSource) {
            try {
                ((CommandSourceBridge) source).bridge$setCurrentCommand((CommandNode<?>) (Object) this);
                cir.setReturnValue(requirement.test(source));
            } finally {
                ((CommandSourceBridge) source).bridge$setCurrentCommand(null);
            }
        }
    }
}
