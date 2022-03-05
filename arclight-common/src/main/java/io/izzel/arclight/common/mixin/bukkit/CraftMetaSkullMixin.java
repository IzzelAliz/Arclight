package io.izzel.arclight.common.mixin.bukkit;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import org.bukkit.craftbukkit.v.inventory.CraftMetaItem;
import org.bukkit.craftbukkit.v.inventory.CraftMetaSkull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(CraftMetaSkull.class)
public class CraftMetaSkullMixin extends CraftMetaItem {

    public CraftMetaSkullMixin(CraftMetaItem meta) {
        super(meta);
    }

    @Redirect(method = "applyToItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/nbt/CompoundTag;put(Ljava/lang/String;Lnet/minecraft/nbt/Tag;)Lnet/minecraft/nbt/Tag;"))
    private Tag arclight$fixNullValue(CompoundTag instance, String key, Tag value) {
        return value == null ? null : instance.put(key, value);
    }
}