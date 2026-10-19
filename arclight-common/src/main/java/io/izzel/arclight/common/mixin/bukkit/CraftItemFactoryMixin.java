package io.izzel.arclight.common.mixin.bukkit;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.item.ItemStack;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import io.izzel.arclight.common.bridge.bukkit.MaterialBridge;
import io.izzel.arclight.i18n.conf.MaterialPropertySpec;
import org.bukkit.Material;
import org.bukkit.craftbukkit.inventory.CraftItemFactory;
import org.bukkit.craftbukkit.inventory.CraftMetaItem;
import org.bukkit.craftbukkit.util.CraftLegacy;
import org.bukkit.inventory.meta.ItemMeta;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = CraftItemFactory.class, remap = false)
public class CraftItemFactoryMixin {

    @WrapOperation(method = "createItemStack(Ljava/lang/String;)Lorg/bukkit/inventory/ItemStack;",
        at = @At(value = "INVOKE", target = "Lorg/bukkit/craftbukkit/inventory/CraftItemStack;asCraftMirror(Lnet/minecraft/world/item/ItemStack;)Lorg/bukkit/craftbukkit/inventory/CraftItemStack;"),
        require = 1, expect = 1, allow = 1)
    private CraftItemStack arclight$validateParsedItem(ItemStack stack, Operation<CraftItemStack> original) {
        // New ItemParser no longer validates the composed item before returning.
        // Preserve the factory's construction path, but reject invalid /give components.
        ItemStack.validateStrict(stack).getOrThrow(IllegalArgumentException::new);
        return original.call(stack);
    }

    @SuppressWarnings("AmbiguousMixinReference")
    @Inject(method = "getItemMeta*", require = 0, expect = 0, cancellable = true, at = @At("HEAD"))
    private void arclight$getItemMeta(Material material, CraftMetaItem meta, CallbackInfoReturnable<ItemMeta> cir) {
        MaterialBridge bridge = (MaterialBridge) (Object) CraftLegacy.fromLegacy(material);
        if (bridge.bridge$getType() != MaterialPropertySpec.MaterialType.VANILLA) {
            cir.setReturnValue(bridge.bridge$itemMetaFactory().apply(meta));
        }
    }
}
