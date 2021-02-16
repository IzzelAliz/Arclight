package io.izzel.arclight.common.mixin.bukkit;

import io.izzel.arclight.common.bridge.bukkit.ItemMetaBridge;
import io.izzel.arclight.common.bridge.bukkit.MaterialBridge;
import io.izzel.arclight.common.bridge.item.ItemStackBridge;
import io.izzel.arclight.i18n.conf.MaterialPropertySpec;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v.inventory.CraftItemFactory;
import org.bukkit.craftbukkit.v.inventory.CraftItemStack;
import org.bukkit.craftbukkit.v.inventory.CraftMetaItem;
import org.bukkit.inventory.meta.ItemMeta;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = CraftItemStack.class, remap = false)
public abstract class CraftItemStackMixin {

    // @formatter:off
    @Shadow static Material getType(ItemStack item) { return null; }
    @Shadow static boolean hasItemMeta(ItemStack item) { return false; }
    // @formatter:on

    @Inject(method = "getItemMeta(Lnet/minecraft/item/ItemStack;)Lorg/bukkit/inventory/meta/ItemMeta;",
        cancellable = true, at = @At("HEAD"))
    private static void arclight$offerCaps(ItemStack item, CallbackInfoReturnable<ItemMeta> cir) {
        Material type = getType(item);
        if (((MaterialBridge) (Object) type).bridge$getType() != MaterialPropertySpec.MaterialType.VANILLA) {
            if (hasItemMeta(item)) {
                CraftMetaItem metaItem = new CraftMetaItem(item.getTag());
                ((ItemMetaBridge) metaItem).bridge$offerUnhandledTags(item.getTag());
                ((ItemMetaBridge) metaItem).bridge$setForgeCaps(((ItemStackBridge) (Object) item).bridge$getForgeCaps());
                cir.setReturnValue(metaItem);
            } else {
                cir.setReturnValue(CraftItemFactory.instance().getItemMeta(getType(item)));
            }
        }
    }

    @Inject(method = "setItemMeta(Lnet/minecraft/item/ItemStack;Lorg/bukkit/inventory/meta/ItemMeta;)Z", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/ItemStack;convertStack(I)V"))
    private static void arclight$setCaps(ItemStack item, ItemMeta itemMeta, CallbackInfoReturnable<Boolean> cir) {
        CompoundNBT forgeCaps = ((ItemMetaBridge) itemMeta).bridge$getForgeCaps();
        if (forgeCaps != null) {
            ((ItemStackBridge)(Object) item).bridge$setForgeCaps(forgeCaps.copy());
        }
    }
}
