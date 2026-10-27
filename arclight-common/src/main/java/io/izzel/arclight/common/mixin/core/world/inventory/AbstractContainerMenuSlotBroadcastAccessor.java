package io.izzel.arclight.common.mixin.core.world.inventory;

import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.function.Supplier;

/** Reuses the native listener and remote-state algorithms for selected inventory slots. */
@Mixin(AbstractContainerMenu.class)
public interface AbstractContainerMenuSlotBroadcastAccessor {

    @Invoker("triggerSlotListeners")
    void arclight$triggerSlotListeners(int rawSlot, ItemStack stack, Supplier<ItemStack> copy);

    @Invoker("synchronizeSlotToRemote")
    void arclight$synchronizeSlotToRemote(int rawSlot, ItemStack stack, Supplier<ItemStack> copy);
}
