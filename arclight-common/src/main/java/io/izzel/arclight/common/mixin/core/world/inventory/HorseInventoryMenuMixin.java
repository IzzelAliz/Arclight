package io.izzel.arclight.common.mixin.core.world.inventory;

import io.izzel.arclight.common.bridge.core.world.entity.player.PlayerBridge;
import io.izzel.arclight.common.bridge.core.world.IInventoryBridge;
import net.minecraft.world.Container;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractMountInventoryMenu;
import org.bukkit.craftbukkit.inventory.CraftInventoryView;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractMountInventoryMenu.class)
public abstract class HorseInventoryMenuMixin extends AbstractContainerMenuMixin {

    // @formatter:off
    @Shadow @Final protected Container mountContainer;
    // @formatter:on

    CraftInventoryView<AbstractMountInventoryMenu, ?> view; // Paper added
    Inventory inventory; // Paper added

    @Inject(method = "<init>", at = @At("RETURN"))
    public void arclight$init(int i, Inventory inventory, Container container, LivingEntity livingEntity, CallbackInfo ci) {
        this.inventory = inventory;
    }

    @Override
    public CraftInventoryView<AbstractMountInventoryMenu, ?> getBukkitView() {
        if (this.view != null) {
            return this.view;
        }
        return this.view = new CraftInventoryView<>(((PlayerBridge) this.inventory.player).bridge$getBukkitEntity(),
            ((IInventoryBridge) this.mountContainer).getOwner().getInventory(), (AbstractMountInventoryMenu) (Object) this);
    }
}
