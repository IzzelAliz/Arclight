package io.izzel.arclight.common.mixin.core.world.inventory;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import io.izzel.arclight.common.bridge.core.world.inventory.PrepareResultMenuBridge;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.ItemCombinerMenu;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ItemCombinerMenu.class)
public abstract class ItemCombinerMenuPrepareResultMixin implements PrepareResultMenuBridge {

    @Shadow @Final protected Container inputSlots;
    @Shadow public abstract int getResultSlot();

    @Unique private boolean arclight$prepareResultDeferred;

    @Override
    public boolean bridge$isPrepareResultDeferred() {
        return this.arclight$prepareResultDeferred;
    }

    @Override
    public void bridge$setPrepareResultDeferred(boolean deferred) {
        this.arclight$prepareResultDeferred = deferred;
    }

    @WrapMethod(method = "slotsChanged(Lnet/minecraft/world/Container;)V", require = 1, allow = 1)
    private void arclight$prepareCombinedResult(Container changed, Operation<Void> original) {
        boolean previouslyDeferred = this.arclight$prepareResultDeferred;
        this.arclight$prepareResultDeferred = true;
        try {
            original.call(changed);
            if (changed == this.inputSlots && !previouslyDeferred) {
                // The subclass calculation and loader hooks have completed. A
                // SmithingMenu scope owns its later recipe-error completion instead.
                CraftEventFactory.callPrepareResultEvent((ItemCombinerMenu) (Object) this, this.getResultSlot());
            }
        } finally {
            this.arclight$prepareResultDeferred = previouslyDeferred;
        }
    }
}
