package io.izzel.arclight.common.mixin.core.resources;

import io.izzel.arclight.common.bridge.core.resources.RegistryLoadTaskBridge;
import io.papermc.paper.registry.data.util.Conversions;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceManagerRegistryLoadTask;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ResourceManagerRegistryLoadTask.class)
public class ResourceManagerRegistryLoadTaskMixin {
    @Inject(method = "load", at = @At("HEAD"))
    private void arclight$conversions(RegistryOps.RegistryInfoLookup lookup, java.util.concurrent.Executor executor,
            CallbackInfoReturnable<java.util.concurrent.CompletableFuture<?>> cir) {
        ((RegistryLoadTaskBridge) this).bridge$conversions(new Conversions(lookup));
    }
}
