package io.izzel.arclight.common.mixin.core.resources;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.izzel.arclight.common.bridge.core.resources.RegistryLoadTaskBridge;
import io.papermc.paper.registry.PaperRegistryAccess;
import io.papermc.paper.registry.PaperRegistryListenerManager;
import io.papermc.paper.registry.data.util.Conversions;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistrationInfo;
import net.minecraft.core.WritableRegistry;
import net.minecraft.resources.RegistryLoadTask;
import net.minecraft.resources.ResourceKey;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RegistryLoadTask.class)
public abstract class RegistryLoadTaskMixin<T> implements RegistryLoadTaskBridge {
    @Shadow @Final private WritableRegistry<T> registry;
    @Unique private Conversions arclight$conversions;

    @Override
    public void bridge$conversions(Conversions conversions) {
        this.arclight$conversions = conversions;
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void arclight$registerRegistry(CallbackInfo ci) {
        PaperRegistryAccess.instance().registerRegistry(this.registry);
    }

    @WrapOperation(method = "lambda$registerElements$1", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/core/WritableRegistry;register(Lnet/minecraft/resources/ResourceKey;Ljava/lang/Object;Lnet/minecraft/core/RegistrationInfo;)Lnet/minecraft/core/Holder$Reference;"))
    private Holder.Reference<T> arclight$register(WritableRegistry<T> registry, ResourceKey<T> key, Object value,
            RegistrationInfo info, Operation<Holder.Reference<T>> original) {
        if (this.arclight$conversions == null) return original.call(registry, key, value, info);
        return PaperRegistryListenerManager.INSTANCE.registerWithListeners(registry, key, (T) value, info,
            (target, id, entry, registration) -> original.call(target, id, entry, registration), this.arclight$conversions);
    }

    @Inject(method = "registerElements", at = @At("RETURN"))
    private void arclight$compose(java.util.stream.Stream<?> entries, CallbackInfo ci) {
        if (this.arclight$conversions != null) {
            PaperRegistryAccess.instance().lockReferenceHolders(this.registry.key());
            PaperRegistryListenerManager.INSTANCE.runFreezeListeners(this.registry.key(), this.arclight$conversions);
        }
    }
}
