package io.izzel.arclight.common.mixin.core.core.registries;

import io.papermc.paper.registry.PaperRegistryAccess;
import net.minecraft.core.Registry;
import net.minecraft.core.WritableRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.Supplier;

@Mixin(BuiltInRegistries.class)
public class BuiltInRegistriesMixin {

    @Inject(
        method = "internalRegister(Lnet/minecraft/resources/ResourceKey;Lnet/minecraft/core/WritableRegistry;Lnet/minecraft/core/registries/BuiltInRegistries$RegistryBootstrap;)Lnet/minecraft/core/WritableRegistry;",
        at = @At("RETURN"),
        require = 1, expect = 1, allow = 1
    )
    private static <T> void arclight$registerApiRegistry(ResourceKey<? extends Registry<T>> name, WritableRegistry<T> registry,
                                                       @Coerce Object loader, CallbackInfoReturnable<WritableRegistry<T>> cir) {
        // RegistryBootstrap is private; only the actual registry instance is needed here.
        PaperRegistryAccess.instance().registerRegistry(cir.getReturnValue());
    }

    @Inject(method = "createContents()V", at = @At("HEAD"), require = 1, expect = 1, allow = 1)
    private static void arclight$initializeApiRegistries(CallbackInfo ci) {
        // Create the CraftRegistry wrappers before any loader can lock their reference holders.
        try {
            Class.forName(org.bukkit.Registry.class.getName());
        } catch (ClassNotFoundException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Inject(
        method = "lambda$createContents$0(Lnet/minecraft/resources/Identifier;Ljava/util/function/Supplier;)V",
        at = @At("RETURN"),
        require = 1, expect = 1, allow = 1
    )
    private static void arclight$lockApiReferenceHolders(Identifier key, Supplier<?> loader, CallbackInfo ci) {
        PaperRegistryAccess.instance().lockReferenceHolders(ResourceKey.createRegistryKey(key));
    }
    @com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation(method = "freeze", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/core/Registry;freeze()Lnet/minecraft/core/Registry;"))
    private static Registry<?> arclight$compose(Registry<?> registry,
            com.llamalad7.mixinextras.injector.wrapoperation.Operation<Registry<?>> original) {
        if (registry != BuiltInRegistries.REGISTRY) {
            io.papermc.paper.registry.PaperRegistryListenerManager.INSTANCE.runFreezeListeners(
                registry.key(), io.izzel.arclight.common.mod.server.PaperRegistryHooks.staticConversions());
        }
        return original.call(registry);
    }
}
