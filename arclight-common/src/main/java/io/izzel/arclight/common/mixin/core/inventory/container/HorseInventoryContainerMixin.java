package io.izzel.arclight.common.mixin.core.inventory.container;

import io.izzel.arclight.common.bridge.entity.player.PlayerEntityBridge;
import io.izzel.arclight.common.bridge.inventory.IInventoryBridge;
import net.minecraft.entity.passive.horse.AbstractHorseEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.HorseInventoryContainer;
import org.bukkit.craftbukkit.v.inventory.CraftInventoryView;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HorseInventoryContainer.class)
public abstract class HorseInventoryContainerMixin extends ContainerMixin {

    // @formatter:off
    @Shadow @Final private IInventory horseInventory;
    // @formatter:on

    CraftInventoryView bukkitEntity;
    PlayerInventory playerInventory;

    @Inject(method = "<init>", at = @At("RETURN"))
    public void arclight$init(int id, PlayerInventory playerInventory, IInventory horseInventory, AbstractHorseEntity horse, CallbackInfo ci) {
        this.playerInventory = playerInventory;
    }

    @Override
    public CraftInventoryView getBukkitView() {
        if (bukkitEntity != null) {
            return bukkitEntity;
        }
        return bukkitEntity = new CraftInventoryView(((PlayerEntityBridge) playerInventory.player).bridge$getBukkitEntity(),
            ((IInventoryBridge) this.horseInventory).getOwner().getInventory(), (Container) (Object) this);
    }
}
