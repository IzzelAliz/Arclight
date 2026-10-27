package io.izzel.arclight.common.mixin.bukkit;

import io.izzel.arclight.common.bridge.bukkit.ItemMetaBridge;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.craftbukkit.inventory.CraftMetaItem;
import org.bukkit.inventory.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(value = CraftMetaItem.class, remap = false)
public class CraftMetaItemMixin implements ItemMetaBridge {

    // @formatter:off
    @Shadow(remap = false) protected net.minecraft.core.component.DataComponentPatch.Builder unhandledTags;
    @Shadow(remap = false) private ItemStack useRemainder;
    // @formatter:on

    @ModifyArg(method = "<init>(Ljava/util/Map;)V", at = @At(value = "INVOKE", target = "Lorg/bukkit/craftbukkit/configuration/ConfigSerializationUtil;getHolderSet(Ljava/lang/Object;Lnet/minecraft/resources/ResourceKey;)Lnet/minecraft/core/HolderSet;"), index = 0)
    private Object arclight$legacyDamageResistance(Object encoded) {
        // Prior CraftBukkit serialized a named damage-resistance tag without '#'.
        // Paper's holder-set decoder requires that prefix for named tags.
        return encoded instanceof String tag && !tag.startsWith("#") ? "#" + tag : encoded;
    }

    @ModifyExpressionValue(method = "applyToItem", at = @At(value = "INVOKE", target = "Lorg/bukkit/craftbukkit/inventory/CraftMetaItem;hasUseRemainder()Z"))
    private boolean arclight$nonEmptyUseRemainder(boolean present) {
        return present && !CraftItemStack.asNMSCopy(this.useRemainder).isEmpty();
    }

}
