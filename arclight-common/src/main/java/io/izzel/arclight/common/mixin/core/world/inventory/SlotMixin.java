package io.izzel.arclight.common.mixin.core.world.inventory;

import io.izzel.arclight.common.bridge.core.inventory.container.SlotBridge;
import net.minecraft.world.inventory.Slot;
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
