package io.izzel.arclight.common.mixin.bukkit;

import io.izzel.arclight.common.bridge.bukkit.CraftItemStackBridge;
import io.izzel.arclight.common.bridge.core.world.item.ItemStackBridge;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v.inventory.CraftItemStack;
import org.bukkit.craftbukkit.v.inventory.CraftItemType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(value = CraftItemStack.class, remap = false)
public abstract class CraftItemStackMixin extends org.bukkit.inventory.ItemStack implements CraftItemStackBridge {

    // @formatter:off
    @Shadow ItemStack handle;
    @Shadow public abstract Material getType();
    @Shadow public abstract short getDurability();
    @Shadow public abstract boolean hasItemMeta();
    // @formatter:on

    /**
     * @author InitAuther97
     * @reason reduce CraftItemType#bukkitToMinecraft call usage.
     */
    @Overwrite
    public void setType(Material type) {
        if (this.getType() != type) {
            if (type == Material.AIR) {
                this.handle = null;
            } else {
                final var craftType = CraftItemType.bukkitToMinecraft(type);
                if (craftType == null) {
                    this.handle = null;
                } else if (this.handle == null) {
                    this.handle = new net.minecraft.world.item.ItemStack(craftType, 1);
                } else {
                    ((ItemStackBridge)(Object) this.handle).arclight$setItem(craftType);
                    if (this.hasItemMeta()) {
                        CraftItemStack.setItemMeta(this.handle, CraftItemStack.getItemMeta(this.handle));
                    }
                }
            }

            this.setData(null);
        }
    }

    @Unique
    private ItemEntity arclight$itemEntity;

    @Override
    public void arclight$setItemEntity(ItemEntity entity) {
        arclight$itemEntity = entity;
    }

    @Override
    public ItemEntity arclight$getItemEntity() {
        if (arclight$itemEntity != null) {
            arclight$itemEntity.setItem(this.handle);
        }
        return arclight$itemEntity;
    }
}
