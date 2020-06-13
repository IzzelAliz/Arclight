package io.izzel.arclight.common.mixin.core.inventory.container;

import io.izzel.arclight.common.bridge.inventory.container.SlotBridge;
import net.minecraft.inventory.container.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(Slot.class)
public abstract class SlotMixin implements SlotBridge {

    // @formatter:off
    @Shadow protected abstract void onSwapCraft(int numItemsCrafted);
    // @formatter:on

    @Override
    public void bridge$onSwapCraft(int numItemsCrafted) {
        onSwapCraft(numItemsCrafted);
    }
}
