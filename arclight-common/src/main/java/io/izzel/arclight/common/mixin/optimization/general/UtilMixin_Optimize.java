package io.izzel.arclight.common.mixin.optimization.general;

import net.minecraft.Util;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Mixin(Util.class)
public class UtilMixin_Optimize {

    /**
     * @author IzzelAliz
     * @reason original method allocates tons of garbage
     */
    @Overwrite
    public static <V> CompletableFuture<List<V>> sequence(List<? extends CompletableFuture<? extends V>> futures) {
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .thenApply(v -> {
                var list = new ArrayList<V>(futures.size());
                for (CompletableFuture<? extends V> future : futures) {
                    list.add(future.join());
                }
                return list;
            });
    }
}
