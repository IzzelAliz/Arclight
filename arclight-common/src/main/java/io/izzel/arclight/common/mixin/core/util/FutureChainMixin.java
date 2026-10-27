package io.izzel.arclight.common.mixin.core.util;

import io.izzel.arclight.common.mod.util.CommandMessageTask;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import io.izzel.arclight.mixin.Local;
import net.minecraft.util.FutureChain;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

@Mixin(FutureChain.class)
public abstract class FutureChainMixin {

    @Shadow private volatile boolean closed;

    @Unique private final CommandMessageTask.Pending arclight$resolutions = new CommandMessageTask.Pending();

    @Decorate(method = "append(Ljava/util/concurrent/CompletableFuture;Ljava/util/function/Consumer;)V", at = @At(value = "INVOKE", target = "Ljava/util/concurrent/CompletableFuture;thenCombine(Ljava/util/concurrent/CompletionStage;Ljava/util/function/BiFunction;)Ljava/util/concurrent/CompletableFuture;"), require = 1, allow = 1)
    private CompletableFuture<?> arclight$trackResolver(CompletableFuture<?> predecessor, CompletionStage<?> preparation,
                                                       BiFunction<?, ?, ?> combine,
                                                       @Local(ordinal = 0) Consumer<?> chainedTask) throws Throwable {
        if (chainedTask instanceof CommandMessageTask<?> resolution) {
            this.arclight$resolutions.add(resolution, this.closed);
            resolution.observePredecessor(predecessor);
        }
        return (CompletableFuture<?>) DecorationOps.callsite().invoke(predecessor, preparation, combine);
    }

    @Decorate(method = "append(Ljava/util/concurrent/CompletableFuture;Ljava/util/function/Consumer;)V", at = @At(value = "INVOKE", target = "Ljava/util/concurrent/CompletableFuture;exceptionally(Ljava/util/function/Function;)Ljava/util/concurrent/CompletableFuture;"), require = 1, allow = 1)
    private CompletableFuture<?> arclight$observeResolverLink(CompletableFuture<?> link, Function<?, ?> recovery,
                                                            @Local(ordinal = 0) Consumer<?> chainedTask) throws Throwable {
        if (chainedTask instanceof CommandMessageTask<?> resolution) {
            // Observe before native recovery logs and hides non-cancellation failures.
            resolution.observeLink(link);
        }
        return (CompletableFuture<?>) DecorationOps.callsite().invoke(link, recovery);
    }

    @Decorate(method = "close()V", inject = true, at = @At("RETURN"), require = 1, allow = 1)
    private void arclight$closeResolvers() {
        // Native closed is already visible, including to resolver callbacks reentering append.
        this.arclight$resolutions.close();
    }
}
