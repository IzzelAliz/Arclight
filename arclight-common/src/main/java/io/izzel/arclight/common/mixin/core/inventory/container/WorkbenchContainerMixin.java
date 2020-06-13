package io.izzel.arclight.common.mixin.core.inventory.container;

import io.izzel.arclight.common.bridge.entity.player.PlayerEntityBridge;
import io.izzel.arclight.common.bridge.inventory.container.ContainerBridge;
import io.izzel.arclight.common.bridge.inventory.container.WorkbenchContainerBridge;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.CraftResultInventory;
import net.minecraft.inventory.CraftingInventory;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.WorkbenchContainer;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.ICraftingRecipe;
import net.minecraft.item.crafting.IRecipeType;
import net.minecraft.network.play.server.SSetSlotPacket;
import net.minecraft.util.IWorldPosCallable;
import net.minecraft.world.World;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.bukkit.craftbukkit.v.inventory.CraftInventoryCrafting;
import org.bukkit.craftbukkit.v.inventory.CraftInventoryView;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import io.izzel.arclight.common.bridge.inventory.CraftingInventoryBridge;
import io.izzel.arclight.common.mod.util.ArclightCaptures;

import java.util.Optional;

// todo 实现
@Mixin(WorkbenchContainer.class)
public abstract class WorkbenchContainerMixin extends ContainerMixin implements WorkbenchContainerBridge {

    // @formatter:off
    @Mutable @Shadow @Final private CraftingInventory craftMatrix;
    @Shadow @Final private CraftResultInventory craftResult;
    @Accessor("worldPosCallable") public abstract IWorldPosCallable bridge$getContainerAccess();
    // @formatter:on

    private CraftInventoryView bukkitEntity;
    private PlayerInventory player;

    @Inject(method = "canInteractWith", cancellable = true, at = @At("HEAD"))
    public void arclight$unreachable(PlayerEntity playerIn, CallbackInfoReturnable<Boolean> cir) {
        if (!bridge$isCheckReachable()) cir.setReturnValue(true);
    }

    private static void a(int p_217066_0_, World p_217066_1_, PlayerEntity p_217066_2_, CraftingInventory p_217066_3_, CraftResultInventory p_217066_4_, Container container) {
        ArclightCaptures.captureWorkbenchContainer(container);
        updateCraftingResult(p_217066_0_, p_217066_1_, p_217066_2_, p_217066_3_, p_217066_4_);
    }

    @Inject(method = "onCraftMatrixChanged", at = @At("HEAD"))
    public void arclight$capture(IInventory inventoryIn, CallbackInfo ci) {
        ArclightCaptures.captureWorkbenchContainer((Container) (Object) this);
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    protected static void updateCraftingResult(int i, World world, PlayerEntity playerEntity, CraftingInventory inventory, CraftResultInventory resultInventory) {
        Container container = ArclightCaptures.getWorkbenchContainer();
        if (!world.isRemote) {
            ServerPlayerEntity serverplayerentity = (ServerPlayerEntity) playerEntity;
            ItemStack itemstack = ItemStack.EMPTY;
            Optional<ICraftingRecipe> optional = world.getServer().getRecipeManager().getRecipe(IRecipeType.CRAFTING, inventory, world);
            if (optional.isPresent()) {
                ICraftingRecipe icraftingrecipe = optional.get();
                if (resultInventory.canUseRecipe(world, serverplayerentity, icraftingrecipe)) {
                    itemstack = icraftingrecipe.getCraftingResult(inventory);
                }
            }

            itemstack = CraftEventFactory.callPreCraftEvent(inventory, resultInventory, itemstack, ((ContainerBridge) container).bridge$getBukkitView(), false);

            resultInventory.setInventorySlotContents(0, itemstack);
            serverplayerentity.connection.sendPacket(new SSetSlotPacket(i, 0, itemstack));
        }
    }

    @Inject(method = "<init>(ILnet/minecraft/entity/player/PlayerInventory;Lnet/minecraft/util/IWorldPosCallable;)V", at = @At("RETURN"))
    public void arclight$init(int i, PlayerInventory playerInventory, IWorldPosCallable callable, CallbackInfo ci) {
        ((CraftingInventoryBridge) this.craftMatrix).bridge$setOwner(playerInventory.player);
        ((CraftingInventoryBridge) this.craftMatrix).bridge$setResultInventory(this.craftResult);
        this.player = playerInventory;
    }

    @Override
    public CraftInventoryView getBukkitView() {
        if (bukkitEntity != null) {
            return bukkitEntity;
        }

        CraftInventoryCrafting inventory = new CraftInventoryCrafting(this.craftMatrix, this.craftResult);
        bukkitEntity = new CraftInventoryView(((PlayerEntityBridge) this.player.player).bridge$getBukkitEntity(), inventory, (Container) (Object) this);
        return bukkitEntity;
    }
}
