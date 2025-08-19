package io.izzel.arclight.common.mixin.core.world.item;

import io.izzel.arclight.common.bridge.core.world.item.ItemStackBridge;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.PatchedDataComponentMap;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ItemStack.class)
public abstract class ItemStackMixin implements ItemStackBridge {

    // @formatter:off
    @Shadow @Final PatchedDataComponentMap components;
    @Shadow @Deprecated @Nullable private Item item;
    // @formatter:on

    public void restorePatch(DataComponentPatch datacomponentpatch) {
        this.components.restorePatch(datacomponentpatch);
    }

    @Override
    public void arclight$restorePatch(DataComponentPatch datacomponentpatch) {
        this.restorePatch(datacomponentpatch);
    }

    @Deprecated
    public void setItem(@Nullable Item item) {
        this.item = item;
    }

    @Override
    public void arclight$setItem(Item item) {
        this.setItem(item);
    }
}
