package io.izzel.arclight.common.mixin.core.inventory.container;

import io.izzel.arclight.common.bridge.entity.player.PlayerEntityBridge;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.container.AbstractFurnaceContainer;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.ContainerType;
import net.minecraft.item.crafting.AbstractCookingRecipe;
import net.minecraft.item.crafting.IRecipeType;
import net.minecraft.tileentity.AbstractFurnaceTileEntity;
import net.minecraft.util.IIntArray;
import org.bukkit.craftbukkit.v.inventory.CraftInventoryFurnace;
import org.bukkit.craftbukkit.v.inventory.CraftInventoryView;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractFurnaceContainer.class)
public abstract class AbstractFurnaceContainerMixin extends ContainerMixin {

    // @formatter:off
    @Shadow @Final private IInventory furnaceInventory;
    // @formatter:on

    private CraftInventoryView bukkitEntity = null;
    private PlayerInventory player;

    @Inject(method = "<init>(Lnet/minecraft/inventory/container/ContainerType;Lnet/minecraft/item/crafting/IRecipeType;ILnet/minecraft/entity/player/PlayerInventory;Lnet/minecraft/inventory/IInventory;Lnet/minecraft/util/IIntArray;)V", at = @At("RETURN"))
    public void arclight$init(ContainerType<?> containerTypeIn, IRecipeType<? extends AbstractCookingRecipe> recipeTypeIn, int id, PlayerInventory playerInventoryIn, IInventory furnaceInventoryIn, IIntArray p_i50104_6_, CallbackInfo ci) {
        this.player = playerInventoryIn;
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

        CraftInventoryFurnace inventory = new CraftInventoryFurnace((AbstractFurnaceTileEntity) this.furnaceInventory);
        bukkitEntity = new CraftInventoryView(((PlayerEntityBridge) this.player.player).bridge$getBukkitEntity(), inventory, (Container) (Object) this);
        return bukkitEntity;
    }
}
