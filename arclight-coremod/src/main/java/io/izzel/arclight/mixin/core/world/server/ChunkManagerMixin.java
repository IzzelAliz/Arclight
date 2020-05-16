package io.izzel.arclight.mixin.core.world.server;

import io.izzel.arclight.bridge.world.server.ChunkManagerBridge;
import net.minecraft.world.server.ChunkManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import io.izzel.arclight.mod.util.ArclightCallbackExecutor;

import java.util.function.BooleanSupplier;

@Mixin(ChunkManager.class)
public abstract class ChunkManagerMixin implements ChunkManagerBridge {

    // @formatter:off
    @Invoker("tick") public abstract void bridge$tick(BooleanSupplier hasMoreTime);
    // @formatter:on

    public final ArclightCallbackExecutor callbackExecutor = new ArclightCallbackExecutor();

    @Override
    public ArclightCallbackExecutor bridge$getCallbackExecutor() {
        return this.callbackExecutor;
    }
}
