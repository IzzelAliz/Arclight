package io.izzel.arclight.common.mixin.core.world.inventory;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import io.izzel.arclight.common.bridge.core.server.level.ServerPlayerBridge;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.CrafterMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.bukkit.craftbukkit.inventory.CraftInventoryCrafter;
import org.bukkit.craftbukkit.inventory.view.CraftCrafterView;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(CrafterMenu.class)
public abstract class CrafterMenuMixin extends AbstractContainerMenuMixin {

    @Shadow @Final private CraftingContainer container;
    @Shadow @Final private ResultContainer resultContainer;
    @Shadow @Final private Player player;
    @Shadow private void refreshRecipeResult() {}

    private CraftCrafterView bukkitEntity;

    @Override
    public CraftCrafterView getBukkitView() {
        if (bukkitEntity != null) {
            return bukkitEntity;
        }

        CraftInventoryCrafter inventory = new CraftInventoryCrafter(this.container, this.resultContainer);
        bukkitEntity = new CraftCrafterView(((ServerPlayerBridge) this.player).bridge$getBukkitEntity(), inventory, (CrafterMenu) (Object) this);
        return bukkitEntity;
    }

    @Override
    public void startOpen() {
        super.startOpen();
        this.refreshRecipeResult();
    }

    @Inject(method = "refreshRecipeResult()V", at = @At("HEAD"), cancellable = true, require = 1, expect = 1, allow = 1)
    private void arclight$deferPreview(CallbackInfo ci) {
        if (!this.opened) ci.cancel();
    }

    @com.llamalad7.mixinextras.injector.ModifyExpressionValue(method = "refreshRecipeResult()V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/CrafterBlock;getPotentialResults(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/crafting/CraftingInput;)Ljava/util/Optional;"), require = 1, expect = 1, allow = 1)
    private Optional<RecipeHolder<CraftingRecipe>> arclight$capturePreviewRecipe(Optional<RecipeHolder<CraftingRecipe>> result,
            @Share("arclight$previewRecipe") LocalRef<Optional<RecipeHolder<CraftingRecipe>>> recipe) {
        recipe.set(result);
        return result;
        // Keep the untouched handler line positions aligned with the sealed baseline
        // so the non-target audit can retain all original debug metadata.
    }

    @WrapOperation(method = "refreshRecipeResult()V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/inventory/ResultContainer;setItem(ILnet/minecraft/world/item/ItemStack;)V"), require = 1, expect = 1, allow = 1)
    private void arclight$preparePreview(ResultContainer resultContainer, int slot, ItemStack result,
            Operation<Void> original,
            @Share("arclight$previewRecipe") LocalRef<Optional<RecipeHolder<CraftingRecipe>>> recipe) {
        ItemStack prepared = CraftEventFactory.callPreCraftEvent(this.container, resultContainer, result, this.getBukkitView(), recipe.get());
        original.call(resultContainer, slot, prepared);
    }

    @Inject(method = "stillValid", cancellable = true, at = @At("HEAD"))
    public void arclight$unreachable(Player playerIn, CallbackInfoReturnable<Boolean> cir) {
        if (!bridge$isCheckReachable()) cir.setReturnValue(true);
    }
}
