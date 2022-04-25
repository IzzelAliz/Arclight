package io.izzel.arclight.common.mixin.optimization.general;

import net.minecraft.resources.ResourceKey;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Mixin(ResourceKey.class)
public class ResourceKeyMixin_Optimize {

    @Redirect(method = "<clinit>", at = @At(value = "INVOKE", remap = false, target = "Ljava/util/Collections;synchronizedMap(Ljava/util/Map;)Ljava/util/Map;"))
    private static <K, V> Map<K, V> arclight$useHashMap(Map<K, V> m) {
        return new ConcurrentHashMap<>();
    }

    @Redirect(method = "create(Lnet/minecraft/resources/ResourceLocation;Lnet/minecraft/resources/ResourceLocation;)Lnet/minecraft/resources/ResourceKey;",
        at = @At(value = "INVOKE", remap = false, target = "Ljava/lang/String;intern()Ljava/lang/String;"))
    private static String arclight$dropIntern(String instance) {
        return instance;
    }
}
