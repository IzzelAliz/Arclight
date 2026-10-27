package io.izzel.arclight.common.mod.server;

import io.papermc.paper.registry.data.util.Conversions;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.RegistryLayer;

public final class PaperRegistryHooks {
    private PaperRegistryHooks() {}

    private static final class StaticConversions {
        private static final Conversions INSTANCE = new Conversions(new RegistryOps.RegistryInfoLookup() {
            @Override
            public <T> java.util.Optional<RegistryOps.RegistryInfo<T>> lookup(net.minecraft.resources.ResourceKey<? extends net.minecraft.core.Registry<? extends T>> key) {
                return io.izzel.arclight.common.mixin.core.server.RegistryLayerAccessor.arclight$staticAccess()
                    .lookup(key).map(RegistryOps.RegistryInfo::fromRegistryLookup);
            }
        });
    }

    public static Conversions staticConversions() {
        return StaticConversions.INSTANCE;
    }
}
