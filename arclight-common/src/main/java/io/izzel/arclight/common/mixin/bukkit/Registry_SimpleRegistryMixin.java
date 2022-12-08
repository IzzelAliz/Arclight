package io.izzel.arclight.common.mixin.bukkit;

import com.google.common.collect.ImmutableMap;
import io.izzel.arclight.common.bridge.bukkit.SimpleRegistryBridge;
import org.bukkit.Keyed;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;
import java.util.function.Predicate;

@Mixin(Registry.SimpleRegistry.class)
public class Registry_SimpleRegistryMixin<T extends Enum<T> & Keyed> implements SimpleRegistryBridge {

    @Shadow @Final @Mutable private Map<NamespacedKey, T> map;

    private Runnable arclight$reloadCallback;

    @Inject(method = "<init>(Ljava/lang/Class;Ljava/util/function/Predicate;)V", at = @At("RETURN"))
    private void arclight$init(Class<T> type, Predicate<T> predicate, CallbackInfo ci) {
        this.arclight$reloadCallback = () -> {
            ImmutableMap.Builder<NamespacedKey, T> builder = ImmutableMap.builder();

            for (T entry : type.getEnumConstants()) {
                if (predicate.test(entry)) {
                    builder.put(entry.getKey(), entry);
                }
            }

            map = builder.build();
        };
    }

    @Override
    public void bridge$reload() {
        this.arclight$reloadCallback.run();
    }
}
