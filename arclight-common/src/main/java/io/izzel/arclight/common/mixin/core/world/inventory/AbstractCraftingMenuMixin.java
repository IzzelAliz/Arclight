package io.izzel.arclight.common.mixin.core.world.inventory;

import net.minecraft.world.inventory.AbstractCraftingMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.ResultContainer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(AbstractCraftingMenu.class)
public abstract class AbstractCraftingMenuMixin extends AbstractContainerMenuMixin {

    // @formatter:off
    @Shadow @Final public CraftingContainer craftSlots;
    @Shadow @Final public ResultContainer resultSlots;
    // @formatter:on
}
