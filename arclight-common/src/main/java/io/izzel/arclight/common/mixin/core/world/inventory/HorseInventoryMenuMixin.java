package io.izzel.arclight.common.mixin.core.world.inventory;

import io.izzel.arclight.common.bridge.core.world.entity.player.PlayerBridge;
import io.izzel.arclight.common.bridge.core.world.IInventoryBridge;
import net.minecraft.world.Container;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.HorseInventoryMenu;
import org.bukkit.craftbukkit.v.inventory.CraftInventoryView;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HorseInventoryMenu.class)
public abstract class HorseInventoryMenuMixin extends AbstractContainerMenuMixin {

    // @formatter:off
    @Shadow @Final private Container horseContainer;
    // @formatter:on

    CraftInventoryView<HorseInventoryMenu, ?> bukkitEntity;
    Inventory playerInventory;

    @Inject(method = "<init>", at = @At("RETURN"))
    public void arclight$init(int i, Inventory inventory, Container container, AbstractHorse abstractHorse, int j, CallbackInfo ci) {
        this.playerInventory = inventory;
    }

    @Override
    public CraftInventoryView<HorseInventoryMenu, ?> getBukkitView() {
        if (bukkitEntity != null) {
            return bukkitEntity;
        }
        return bukkitEntity = new CraftInventoryView<>(((PlayerBridge) playerInventory.player).bridge$getBukkitEntity(),
            ((IInventoryBridge) this.horseContainer).getOwner().getInventory(), (HorseInventoryMenu) (Object) this);
    }
}
