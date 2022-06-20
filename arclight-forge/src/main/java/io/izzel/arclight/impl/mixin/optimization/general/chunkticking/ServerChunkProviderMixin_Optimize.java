package io.izzel.arclight.impl.mixin.optimization.general.chunkticking;

import io.izzel.arclight.common.bridge.world.server.ChunkManagerBridge;
import net.minecraft.world.server.ChunkManager;
import net.minecraft.world.server.ServerChunkProvider;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@Mixin(ServerChunkProvider.class)
public class ServerChunkProviderMixin_Optimize {

    // @formatter:off
    @Shadow @Final public ChunkManager chunkManager;
    // @formatter:on

    private static final ArrayList<?> EMPTY = new ArrayList<>(0);

    @Redirect(method = "Lnet/minecraft/world/server/ServerChunkProvider;tickChunks()V",
    at = @At(value = "INVOKE", target = "Lcom/google/common/collect/Lists;newArrayList(Ljava/lang/Iterable;)Ljava/util/ArrayList;"))
    public ArrayList<?> arclight$removeNewList(Iterable<?> elements) {
        return EMPTY;
    }

    @Redirect(method = "Lnet/minecraft/world/server/ServerChunkProvider;tickChunks()V",
            at = @At(value = "INVOKE", target = "Ljava/util/Collections;shuffle(Ljava/util/List;)V"))
    public void arclight$removeShuffle(List<?> objects) {}

    @Redirect(method = "Lnet/minecraft/world/server/ServerChunkProvider;tickChunks()V",
    at = @At(value = "INVOKE", target = "Ljava/util/List;forEach(Ljava/util/function/Consumer;)V"))
    public void arclight$redirectChunkForEach(List instance, Consumer consumer) {
        ((ChunkManagerBridge) this.chunkManager).bridge$getLoadedChunksIterable().forEach(consumer);
    }
}
