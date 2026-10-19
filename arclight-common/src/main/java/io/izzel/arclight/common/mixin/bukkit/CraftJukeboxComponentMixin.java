package io.izzel.arclight.common.mixin.bukkit;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderOwner;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.JukeboxSong;
import org.bukkit.craftbukkit.inventory.components.CraftJukeboxComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = CraftJukeboxComponent.class, remap = false)
public abstract class CraftJukeboxComponentMixin {

    @WrapOperation(method = {
        "<init>(Ljava/util/Map;)V",
        "setSongKey(Lorg/bukkit/NamespacedKey;)V"
    }, at = @At(value = "INVOKE", target = "Lnet/minecraft/core/Holder$Reference;createStandAlone(Lnet/minecraft/core/HolderOwner;Lnet/minecraft/resources/ResourceKey;)Lnet/minecraft/core/Holder$Reference;"),
        require = 2, expect = 2, allow = 2)
    private static Holder.Reference<JukeboxSong> arclight$registeredSong(HolderOwner<JukeboxSong> owner, ResourceKey<JukeboxSong> key,
                                                                      Operation<Holder.Reference<JukeboxSong>> original) {
        Holder.Reference<JukeboxSong> reference = original.call(owner, key);
        // Normalize only the original unbound key/owner shape; keep other mods' bound or retargeted results.
        if (reference != null && !reference.isBound() && reference.is(key) && reference.canSerializeIn(owner)
            && owner instanceof Registry<JukeboxSong> registry) {
            // Lookup returns the registry's actual Reference; absent keys retain the original standalone.
            return registry.get(key.identifier()).orElse(reference);
        }
        return reference;
    }
}
