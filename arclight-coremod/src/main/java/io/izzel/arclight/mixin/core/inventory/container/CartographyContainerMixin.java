package io.izzel.arclight.mixin.core.inventory.container;

import io.izzel.arclight.bridge.entity.player.ServerPlayerEntityBridge;
import io.izzel.arclight.bridge.inventory.container.CartographyContainerBridge;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.CraftResultInventory;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.container.CartographyContainer;
import net.minecraft.inventory.container.Container;
import net.minecraft.util.IWorldPosCallable;
import org.bukkit.craftbukkit.v1_14_R1.inventory.CraftInventoryCartography;
import org.bukkit.craftbukkit.v1_14_R1.inventory.CraftInventoryView;
import org.bukkit.entity.Player;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CartographyContainer.class)
public class CartographyContainerMixin extends ContainerMixin implements CartographyContainerBridge {

    // @formatter:off
    @Shadow @Final private IWorldPosCallable field_216999_d;
    @Shadow @Final public IInventory field_216998_c;
    @Shadow @Final private CraftResultInventory field_217001_f;
    // @formatter:on

    private CraftInventoryView bukkitEntity = null;
    private Player player;

    @Inject(method = "<init>(ILnet/minecraft/entity/player/PlayerInventory;Lnet/minecraft/util/IWorldPosCallable;)V", at = @At("RETURN"))
    public void arclight$init(int p_i50094_1_, PlayerInventory playerInventory, IWorldPosCallable p_i50094_3_, CallbackInfo ci) {
        this.player = ((ServerPlayerEntityBridge) playerInventory.player).bridge$getBukkitEntity();
    }

    @Inject(method = "canInteractWith", cancellable = true, at = @At("HEAD"))
    public void arclight$unreachable(PlayerEntity playerIn, CallbackInfoReturnable<Boolean> cir) {
        if (!bridge$isCheckReachable()) cir.setReturnValue(true);
    }

    @Override
    public CraftInventoryView getBukkitView() {
        if (bukkitEntity != null) {
            return bukkitEntity;
        }

        CraftInventoryCartography inventory = new CraftInventoryCartography(this.field_216998_c, this.field_217001_f);
        bukkitEntity = new CraftInventoryView(this.player, inventory, (Container) (Object) this);
        return bukkitEntity;
    }

    @Override
    public IWorldPosCallable bridge$getContainerAccess() {
        return this.field_216999_d;
    }
}
