package io.izzel.arclight.common.mixin.optimization.general;

import net.minecraft.Util;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Mixin(Util.class)
public class UtilMixin_Optimize {

    /**
     * @author IzzelAliz
     * @reason original method allocates tons of garbage
     */
    @Overwrite
    public static <V> CompletableFuture<List<V>> sequence(List<? extends CompletableFuture<V>> futures) {
        if (futures.isEmpty()) {
            return CompletableFuture.completedFuture(List.of());
        } else if (futures.size() == 1) {
            return futures.get(0).thenApply(List::of);
        } else {
            return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(it -> futures.stream().map(CompletableFuture::join).toList());
        }
    }
}
