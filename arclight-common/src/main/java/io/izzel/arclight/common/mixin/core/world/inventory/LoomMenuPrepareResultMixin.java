package io.izzel.arclight.common.mixin.core.world.inventory;

import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import net.minecraft.world.inventory.LoomMenu;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LoomMenu.class)
public abstract class LoomMenuPrepareResultMixin {

    @Decorate(method = "slotsChanged(Lnet/minecraft/world/Container;)V", require = 1, allow = 1,
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/inventory/LoomMenu;broadcastChanges()V"))
    private void arclight$prepareLoomResult(LoomMenu menu) throws Throwable {
        // Only the completed banner+dye branch has this callsite. Do not hook
        // broadcastChanges itself: the unified helper broadcasts the edited result.
        CraftEventFactory.callPrepareResultEvent(menu, 3);
        // Retain the native/mod synchronisation continuation; both broadcasts now
        // see the approved result, and neither re-enters this slotsChanged callsite.
        DecorationOps.callsite().invoke(menu);
    }
}
