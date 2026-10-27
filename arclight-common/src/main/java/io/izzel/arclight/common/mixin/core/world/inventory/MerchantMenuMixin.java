package io.izzel.arclight.common.mixin.core.world.inventory;

import io.izzel.arclight.common.bridge.core.world.entity.player.PlayerBridge;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import io.izzel.arclight.mixin.Local;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MerchantContainer;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.Merchant;
import org.bukkit.craftbukkit.inventory.CraftInventoryMerchant;
import org.bukkit.craftbukkit.inventory.view.CraftMerchantView;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MerchantMenu.class)
public abstract class MerchantMenuMixin extends AbstractContainerMenuMixin {

    @Shadow @Final private Merchant trader;
    @Shadow @Final private MerchantContainer tradeContainer;

    @Shadow private void playTradeSound() { throw new AssertionError(); }

    @Decorate(method = "quickMoveStack(Lnet/minecraft/world/entity/player/Player;I)Lnet/minecraft/world/item/ItemStack;",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/inventory/MerchantMenu;moveItemStackTo(Lnet/minecraft/world/item/ItemStack;IIZ)Z", ordinal = 0), require = 1, allow = 1)
    private boolean arclight$takeBeforeMovingResult(MerchantMenu menu, ItemStack stack, int start, int end, boolean backwards,
            @Local(ordinal = 0) Player player, @Local(ordinal = 0) Slot slot,
            @Local(ordinal = 0) ItemStack clicked) throws Throwable {
        // A result must not enter the inventory before the cancellable onTake transaction.
        // Paper's check-only overload is absent on loader targets; mirror its read-only pass here.
        if (!this.arclight$canMoveTradeResult(stack, start, end, backwards)) {
            return (boolean) DecorationOps.cancel().invoke(ItemStack.EMPTY);
        }
        slot.onTake(player, stack);
        if (stack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
            return (boolean) DecorationOps.cancel().invoke(ItemStack.EMPTY);
        }
        // Preserve the current native/mod transfer call once; its result is ignored by Paper's commit pass.
        boolean moved = (boolean) DecorationOps.callsite().invoke(menu, stack, start, end, backwards);
        slot.onQuickCraft(stack, clicked);
        this.playTradeSound();
        slot.set(ItemStack.EMPTY);
        // The native common tail would run onTake again. Return its original clicked snapshot now.
        return (boolean) DecorationOps.cancel().invoke(clicked);
    }

    private boolean arclight$canMoveTradeResult(ItemStack original, int start, int end, boolean backwards) {
        ItemStack stack = original.copy();
        boolean changed = false;
        int index = backwards ? end - 1 : start;
        if (stack.isStackable()) {
            while (!stack.isEmpty() && (backwards ? index >= start : index < end)) {
                Slot targetSlot = this.slots.get(index);
                ItemStack target = targetSlot.getItem().copy();
                if (!target.isEmpty() && ItemStack.isSameItemSameComponents(stack, target)) {
                    int total = target.getCount() + stack.getCount();
                    int maximum = targetSlot.getMaxStackSize(target);
                    if (total <= maximum) {
                        stack.setCount(0);
                        changed = true;
                    } else if (target.getCount() < maximum) {
                        stack.shrink(maximum - target.getCount());
                        changed = true;
                    }
                }
                index += backwards ? -1 : 1;
            }
        }
        if (!stack.isEmpty()) {
            index = backwards ? end - 1 : start;
            while (backwards ? index >= start : index < end) {
                Slot targetSlot = this.slots.get(index);
                ItemStack target = targetSlot.getItem().copy();
                if (target.isEmpty() && targetSlot.mayPlace(stack)) {
                    stack.shrink(Math.min(stack.getCount(), targetSlot.getMaxStackSize(stack)));
                    changed = true;
                    break;
                }
                index += backwards ? -1 : 1;
            }
        }
        return changed;
    }

    private CraftMerchantView bukkitEntity;
    private Inventory inventory;

    @Inject(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/item/trading/Merchant;)V", at = @At("RETURN"))
    public void arclight$init(int id, Inventory inventory, Merchant merchant, CallbackInfo ci) {
        this.inventory = inventory;
    }

    @Inject(method = "playTradeSound", cancellable = true, at = @At("HEAD"))
    public void arclight$returnIfFail(CallbackInfo ci) {
        if (!(this.trader instanceof Entity)) {
            ci.cancel();
        }
    }

    @Inject(method = "stillValid", at = @At("HEAD"), cancellable = true)
    private void arclight$checkReachable(net.minecraft.world.entity.player.Player player, org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<Boolean> cir) {
        if (!this.bridge$isCheckReachable()) {
            cir.setReturnValue(true);
        }
    }

    @Override
    public CraftMerchantView getBukkitView() {
        if (bukkitEntity == null) {
            bukkitEntity = new CraftMerchantView(((PlayerBridge) this.inventory.player).bridge$getBukkitEntity(), new CraftInventoryMerchant(this.trader, this.tradeContainer), (MerchantMenu) (Object) this, trader);
        }
        return bukkitEntity;
    }
}
