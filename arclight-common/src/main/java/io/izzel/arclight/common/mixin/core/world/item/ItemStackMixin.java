package io.izzel.arclight.common.mixin.core.world.item;

import io.izzel.arclight.common.bridge.core.world.item.ItemStackBridge;
import io.izzel.arclight.common.mod.util.ArclightCaptures;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.PatchedDataComponentMap;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ItemStack.class)
public abstract class ItemStackMixin implements ItemStackBridge {

    // @formatter:off
    @Shadow @Final @Mutable PatchedDataComponentMap components;
    @Shadow @Final @Mutable @Deprecated @Nullable private Holder<Item> item;
    // @formatter:on

    @Override
    public void restorePatch(DataComponentPatch datacomponentpatch) {
        this.arclight$restorePatch(datacomponentpatch);
    }

    @Override
    public void arclight$restorePatch(DataComponentPatch datacomponentpatch) {
        this.components.restorePatch(datacomponentpatch);
    }

    @Override
    @Deprecated
    public void setItem(Item item) {
        this.arclight$setItem(item);
    }

    @Deprecated
    public void setItem(@Nullable Holder<Item> item) {
        if (item == null || (Object) this == ItemStack.EMPTY) {
            return;
        }
        if (this.bridge$canSetItem(item)) {
            this.item = item;
            DataComponentPatch patch = ((ItemStack) (Object) this).getComponentsPatch();
            this.components = new PatchedDataComponentMap(item.components());
            this.components.applyPatch(patch);
        }
    }

    @Override
    public void arclight$setItem(Item item) {
        this.setItem(item == null ? null : item.builtInRegistryHolder());
    }

    @Override
    public org.bukkit.inventory.ItemStack asBukkitCopy() {
        return CraftItemStack.asBukkitCopy((ItemStack) (Object) this);
    }

    @Override
    public org.bukkit.inventory.ItemStack asBukkitMirror() {
        return CraftItemStack.asCraftMirror((ItemStack) (Object) this);
    }

    @Override
    public void bridge$hurtAndBreak(int amount, LivingEntity owner, @Nullable EquipmentSlot slot, boolean force) {
        if (owner.level() instanceof ServerLevel level) {
            ArclightCaptures.withItemDamage((ItemStack) (Object) this, owner, force,
                () -> this.bridge$platform$hurtAndBreak(amount, level, owner, brokenItem -> {
                    if (slot != null) {
                        owner.onEquippedItemBroken(brokenItem, slot);
                    }
                }));
        }
    }

    public void hurtAndBreak(int amount, net.minecraft.world.entity.LivingEntity owner, net.minecraft.world.entity.EquipmentSlot slot, boolean force) {
        this.bridge$hurtAndBreak(amount, owner, slot, force);
    }

}
