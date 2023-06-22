package io.izzel.arclight.common.mixin.bukkit;

import com.google.common.collect.BiMap;
import org.bukkit.craftbukkit.v.util.CraftMagicNumbers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = CraftMagicNumbers.class, remap = false)
public class CraftMagicNumbersMixin {

    @Redirect(method = "<clinit>", at = @At(value = "INVOKE", target = "Lcom/google/common/collect/BiMap;put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;"))
    private static <K, V> V arclight$skip(BiMap<K, V> instance, K k, V v) {
        if (v == null) {
            return null;
        }
        return instance.put(k, v);
    }
}
