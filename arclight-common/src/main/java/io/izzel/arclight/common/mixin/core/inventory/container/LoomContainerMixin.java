package io.izzel.arclight.common.mixin.core.inventory.container;

import io.izzel.arclight.common.bridge.entity.player.PlayerEntityBridge;
import io.izzel.arclight.common.bridge.inventory.container.PosContainerBridge;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.LoomContainer;
import net.minecraft.util.IWorldPosCallable;
import org.bukkit.craftbukkit.v.inventory.CraftInventoryLoom;
import org.bukkit.craftbukkit.v.inventory.CraftInventoryView;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LoomContainer.class)
public abstract class LoomContainerMixin extends ContainerMixin implements PosContainerBridge {

    // @formatter:off
    @Shadow @Final private IInventory inputInventory;
    @Shadow @Final private IInventory outputInventory;
    @Shadow @Final private IWorldPosCallable worldPos;
    // @formatter:on

    private CraftInventoryView bukkitEntity;
    private PlayerInventory playerInventory;

    @Inject(method = "<init>(ILnet/minecraft/entity/player/PlayerInventory;Lnet/minecraft/util/IWorldPosCallable;)V", at = @At("RETURN"))
    public void arclight$init(int id, PlayerInventory playerInventory, IWorldPosCallable worldCallable, CallbackInfo ci) {
        this.playerInventory = playerInventory;
    }

    @Inject(method = "canInteractWith", at = @At("HEAD"))
    public void arclight$unreachable(PlayerEntity playerIn, CallbackInfoReturnable<Boolean> cir) {
        if (!bridge$isCheckReachable()) {
            cir.setReturnValue(true);
        }
    }

    @Override
    public CraftInventoryView getBukkitView() {
        if (bukkitEntity != null) {
            return bukkitEntity;
        }

        CraftInventoryLoom inventory = new CraftInventoryLoom(this.inputInventory, this.outputInventory);
        bukkitEntity = new CraftInventoryView(((PlayerEntityBridge) this.playerInventory.player).bridge$getBukkitEntity(), inventory, (Container) (Object) this);
        return bukkitEntity;
    }

    @Override
    public IWorldPosCallable bridge$getWorldPos() {
        return this.worldPos;
    }
}
