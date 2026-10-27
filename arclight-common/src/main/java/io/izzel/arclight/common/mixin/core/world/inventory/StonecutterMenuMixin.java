package io.izzel.arclight.common.mixin.core.world.inventory;

import io.izzel.arclight.common.bridge.core.world.entity.player.PlayerBridge;
import io.izzel.arclight.common.bridge.core.world.inventory.PosContainerBridge;
import io.izzel.arclight.common.bridge.core.world.item.crafting.RecipeHolderBridge;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import io.izzel.arclight.mixin.Local;
import io.papermc.paper.event.player.PlayerStonecutterRecipeSelectEvent;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.StonecutterMenu;
import net.minecraft.world.item.crafting.SelectableRecipe;
import net.minecraft.world.item.crafting.StonecutterRecipe;
import org.bukkit.craftbukkit.inventory.CraftInventoryStonecutter;
import org.bukkit.craftbukkit.inventory.view.CraftStonecutterView;
import org.bukkit.craftbukkit.util.CraftNamespacedKey;
import org.bukkit.inventory.StonecuttingRecipe;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(StonecutterMenu.class)
public abstract class StonecutterMenuMixin extends AbstractContainerMenuMixin implements PosContainerBridge {

    // @formatter:off
    @Shadow @Final public Container container;
    @Shadow @Final private ResultContainer resultContainer;
    @Shadow @Final private ContainerLevelAccess access;
    @Shadow @Final private DataSlot selectedRecipeIndex;
    @Shadow private SelectableRecipe.SingleInputSet<StonecutterRecipe> recipesForInput;
    @Shadow private boolean isValidRecipeIndex(int index) { throw new AssertionError(); }
    // @formatter:on

    @Decorate(method = "clickMenuButton(Lnet/minecraft/world/entity/player/Player;I)Z", require = 1, allow = 1,
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/inventory/StonecutterMenu;setupResultSlot(I)V"))
    private void arclight$selectRecipe(StonecutterMenu menu, int index, @Local(ordinal = 0) Player player) throws Throwable {
        // The native valid-index branch has already published the clicked selection.
        this.selectedRecipeIndex.checkAndClearUpdateFlag();
        int recipeIndex = index;
        if (this.isValidRecipeIndex(index)) {
            var recipe = this.recipesForInput.entries().get(index).recipe().recipe();
            if (recipe.isPresent()
                && ((RecipeHolderBridge) (Object) recipe.get()).bridge$toBukkitRecipe() instanceof StonecuttingRecipe bukkitRecipe) {
                var event = new PlayerStonecutterRecipeSelectEvent((org.bukkit.entity.Player) ((PlayerBridge) player).bridge$getBukkitEntity(),
                    this.getBukkitView().getTopInventory(), bukkitRecipe);
                if (!event.callEvent()) {
                    player.containerMenu.sendAllDataToRemote();
                    // Paper keeps the clicked index, but does not rebuild the result on veto.
                    DecorationOps.cancel().invoke(false);
                    return;
                }

                var key = CraftNamespacedKey.toMinecraft(event.getStonecuttingRecipe().getKey());
                if (!recipe.get().id().identifier().equals(key)) {
                    for (int newRecipeIndex = 0; newRecipeIndex < this.recipesForInput.entries().size(); newRecipeIndex++) {
                        if (this.recipesForInput.entries().get(newRecipeIndex).recipe().recipe()
                            .filter(candidate -> candidate.id().identifier().equals(key)).isPresent()) {
                            recipeIndex = newRecipeIndex;
                            break;
                        }
                    }
                }
            }
        }
        player.containerMenu.sendAllDataToRemote();
        this.selectedRecipeIndex.set(recipeIndex);
        DecorationOps.callsite().invoke(menu, recipeIndex);
    }

    @Inject(method = "stillValid(Lnet/minecraft/world/entity/player/Player;)Z", at = @At("HEAD"), cancellable = true)
    private void arclight$checkReachable(net.minecraft.world.entity.player.Player player, org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<Boolean> cir) {
        if (!this.bridge$isCheckReachable()) {
            cir.setReturnValue(true);
        }
    }

    private CraftStonecutterView bukkitEntity = null;
    private Inventory playerInventory;

    @Inject(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/inventory/ContainerLevelAccess;)V", at = @At("RETURN"))
    public void arclight$init(int windowIdIn, Inventory playerInventoryIn, ContainerLevelAccess worldPosCallableIn, CallbackInfo ci) {
        this.playerInventory = playerInventoryIn;
    }

    @Override
    public CraftStonecutterView getBukkitView() {
        if (bukkitEntity != null) {
            return bukkitEntity;
        }

        CraftInventoryStonecutter inventory = new CraftInventoryStonecutter(this.container, this.resultContainer);
        bukkitEntity = new CraftStonecutterView(((PlayerBridge) this.playerInventory.player).bridge$getBukkitEntity(), inventory, (StonecutterMenu) (Object) this);
        return bukkitEntity;
    }

    @Override
    public ContainerLevelAccess bridge$getWorldPos() {
        return this.access;
    }
}
