package io.izzel.arclight.common.mixin.core.world.inventory;

import io.izzel.arclight.common.bridge.core.entity.player.PlayerEntityBridge;
import io.izzel.arclight.common.bridge.core.inventory.IInventoryBridge;
import net.minecraft.world.Container;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.HorseInventoryMenu;
import org.bukkit.craftbukkit.v.inventory.CraftInventoryView;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HorseInventoryMenu.class)
public abstract class HorseInventoryContainerMixin extends AbstractContainerMenuMixin {

    // @formatter:off
    @Shadow @Final private Container horseContainer;
    // @formatter:on

    CraftInventoryView bukkitEntity;
    Inventory playerInventory;

    @Inject(method = "<init>", at = @At("RETURN"))
    public void arclight$init(int id, Inventory playerInventory, Container horseInventory, AbstractHorse horse, CallbackInfo ci) {
        this.playerInventory = playerInventory;
    }

    @Override
    public CraftInventoryView getBukkitView() {
        if (bukkitEntity != null) {
            return bukkitEntity;
        }
        return bukkitEntity = new CraftInventoryView(((PlayerEntityBridge) playerInventory.player).bridge$getBukkitEntity(),
            ((IInventoryBridge) this.horseContainer).getOwner().getInventory(), (AbstractContainerMenu) (Object) this);
    }
}
