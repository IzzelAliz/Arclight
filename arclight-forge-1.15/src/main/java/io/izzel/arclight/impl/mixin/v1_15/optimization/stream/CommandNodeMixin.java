package io.izzel.arclight.impl.mixin.v1_15.optimization.stream;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.RedirectModifier;
import com.mojang.brigadier.tree.CommandNode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;
import java.util.TreeMap;
import java.util.function.Predicate;

@Mixin(value = CommandNode.class, remap = false)
public class CommandNodeMixin<S> {

    // @formatter:off
    @Shadow private Map<String, CommandNode<S>> children;
    // @formatter:on

    @Inject(method = "<init>", at = @At("RETURN"))
    private void arclight$init(Command<S> command, Predicate<S> requirement, CommandNode<S> redirect, RedirectModifier<S> modifier, boolean forks, CallbackInfo ci) {
        this.children = new TreeMap<>();
    }

    @Inject(method = "addChild", cancellable = true, at = @At(value = "INVOKE", target = "Ljava/util/Map;entrySet()Ljava/util/Set;"))
    private void arclight$skipSort(CommandNode<S> node, CallbackInfo ci) {
        ci.cancel();
    }
}
