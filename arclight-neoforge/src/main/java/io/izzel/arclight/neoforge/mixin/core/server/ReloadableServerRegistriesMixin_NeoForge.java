package io.izzel.arclight.neoforge.mixin.core.server;

import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import io.izzel.arclight.mixin.Local;
import io.izzel.arclight.common.mixin.core.resources.RegistryOpsAccessor;
import io.papermc.paper.registry.PaperRegistryAccess;
import io.papermc.paper.registry.PaperRegistryListenerManager;
import io.papermc.paper.registry.data.util.Conversions;
import java.util.Map;
import java.util.function.BiConsumer;
import net.minecraft.core.RegistrationInfo;
import net.minecraft.core.WritableRegistry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.ReloadableServerRegistries;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.level.storage.loot.LootDataType;
import net.minecraft.world.level.storage.loot.Validatable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ReloadableServerRegistries.class)
public class ReloadableServerRegistriesMixin_NeoForge {
    @Shadow @Final private static RegistrationInfo DEFAULT_REGISTRATION_INFO;

    @Decorate(method = "lambda$scheduleRegistryLoad$0", at = @At(value = "INVOKE",
        target = "Ljava/util/Map;forEach(Ljava/util/function/BiConsumer;)V", ordinal = 1))
    private static <T extends Validatable> void arclight$entries(Map<Identifier, T> entries, BiConsumer<Identifier, T> action, LootDataType<T> type, RegistryOps<?> ops, ResourceManager resources,
            @Local(ordinal = 0) WritableRegistry<T> registry) throws Throwable {
        PaperRegistryAccess.instance().registerReloadableRegistry(registry);
        Conversions conversions = new Conversions(((RegistryOpsAccessor) ops).arclight$lookupProvider());
        DecorationOps.callsite().invoke(entries, (BiConsumer<Identifier, T>) (id, value) ->
            PaperRegistryListenerManager.INSTANCE.registerWithListeners(registry,
                ResourceKey.create(type.registryKey(), id), value, DEFAULT_REGISTRATION_INFO, conversions));
    }
}
