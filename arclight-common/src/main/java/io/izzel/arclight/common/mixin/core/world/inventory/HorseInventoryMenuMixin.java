package io.izzel.arclight.common.mixin.core.world.inventory;

import io.izzel.arclight.common.bridge.core.world.entity.player.PlayerBridge;
import io.izzel.arclight.common.bridge.core.world.IInventoryBridge;
import net.minecraft.world.Container;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractMountInventoryMenu;
import org.bukkit.craftbukkit.v.inventory.CraftInventoryView;
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

    CraftInventoryView<AbstractMountInventoryMenu, ?> bukkitEntity;
    Inventory playerInventory;

    @Inject(method = "<init>", at = @At("RETURN"))
    public void arclight$init(int i, Inventory inventory, Container container, LivingEntity livingEntity, CallbackInfo ci) {
        this.playerInventory = inventory;
    }

    @Override
    public CraftInventoryView<AbstractMountInventoryMenu, ?> getBukkitView() {
        if (bukkitEntity != null) {
            return bukkitEntity;
        }
        return bukkitEntity = new CraftInventoryView<>(((PlayerBridge) playerInventory.player).bridge$getBukkitEntity(),
            ((IInventoryBridge) this.mountContainer).getOwner().getInventory(), (AbstractMountInventoryMenu) (Object) this);
    }
}
