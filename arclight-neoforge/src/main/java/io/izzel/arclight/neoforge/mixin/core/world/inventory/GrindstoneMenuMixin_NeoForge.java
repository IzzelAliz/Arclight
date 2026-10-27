package io.izzel.arclight.neoforge.mixin.core.world.inventory;

import net.minecraft.world.inventory.GrindstoneMenu;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GrindstoneMenu.class)
public abstract class GrindstoneMenuMixin_NeoForge {
    @Shadow private int xp;

    @Inject(method = "createResult", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/inventory/GrindstoneMenu;broadcastChanges()V"), order = 900)
    private void arclight$prepareModResult(CallbackInfo ci) {
        if (this.xp != Integer.MIN_VALUE) {
            CraftEventFactory.callPrepareResultEvent((GrindstoneMenu) (Object) this, GrindstoneMenu.RESULT_SLOT);
        }
    }
}
