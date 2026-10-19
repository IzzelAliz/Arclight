package io.izzel.arclight.common.mixin.bukkit;

import io.izzel.arclight.common.bridge.bukkit.ItemMetaBridge;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.damagesource.DamageType;
import org.bukkit.craftbukkit.CraftRegistry;
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
    @Shadow(remap = false) private Integer additionalTradeCost;
    @Shadow(remap = false) private ItemStack useRemainder;
    // @formatter:on

    @ModifyArg(method = "<init>(Ljava/util/Map;)V", at = @At(value = "INVOKE", target = "Lorg/bukkit/craftbukkit/inventory/components/CraftHolderUtil;parse(Ljava/lang/Object;Lnet/minecraft/resources/ResourceKey;Lnet/minecraft/core/Registry;)Lnet/minecraft/core/HolderSet;"), index = 0)
    private Object arclight$legacyDamageResistance(Object encoded) {
        // Prior CraftBukkit serialized a named damage-resistance tag without '#'. New
        // CraftHolderUtil.parse treats bare strings as a singleton damage-type ID.
        return encoded instanceof String tag && !tag.startsWith("#") ? "#" + tag : encoded;
    }

    @ModifyExpressionValue(method = "getDamageType", at = @At(value = "FIELD", target = "Lorg/bukkit/craftbukkit/inventory/CraftMetaItem;damageType:Lnet/minecraft/core/Holder;"))
    private Holder<DamageType> arclight$resolvedDamageType(Holder<DamageType> holder) {
        if (holder instanceof Holder.Reference<DamageType> reference && !reference.isBound()) {
            // Preserve unresolved-key storage; only resolve registered keys at value consumption.
            return CraftRegistry.getMinecraftRegistry(Registries.DAMAGE_TYPE).get(reference.key().identifier()).orElse(reference);
        }
        return holder;
    }

    @ModifyExpressionValue(method = "applyToItem", at = @At(value = "INVOKE", target = "Lorg/bukkit/craftbukkit/inventory/CraftMetaItem;hasUseRemainder()Z"))
    private boolean arclight$nonEmptyUseRemainder(boolean present) {
        return present && !CraftItemStack.asNMSCopy(this.useRemainder).isEmpty();
    }

    @ModifyExpressionValue(method = "getAdditionalTradeCost", at = @At(value = "FIELD", target = "Lorg/bukkit/craftbukkit/inventory/CraftMetaItem;maxStackSize:Ljava/lang/Integer;"))
    private Integer arclight$additionalTradeCost(Integer original) {
        return this.additionalTradeCost;
    }
}
