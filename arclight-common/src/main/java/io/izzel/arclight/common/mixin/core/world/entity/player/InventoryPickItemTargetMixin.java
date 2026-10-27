package io.izzel.arclight.common.mixin.core.world.entity.player;

import io.izzel.arclight.common.mod.util.ArclightCaptures;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import net.minecraft.world.entity.player.Inventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Inventory.class)
public abstract class InventoryPickItemTargetMixin {

    @Decorate(method = {"pickSlot(I)V", "addAndPickItem(Lnet/minecraft/world/item/ItemStack;)V"},
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Inventory;getSuitableHotbarSlot()I"),
        require = 2, allow = 2)
    private int arclight$pickItemTarget(Inventory inventory) throws Throwable {
        int nativeSlot = (int) DecorationOps.callsite().invoke(inventory);
        var target = ArclightCaptures.pickItemTarget;
        return target != null && target.getFirst() == inventory && target.getSecond() >= 0 ? target.getSecond() : nativeSlot;
    }
}
