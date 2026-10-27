package io.izzel.arclight.common.mixin.core.world.inventory;

import io.izzel.arclight.common.bridge.core.server.level.ServerPlayerBridge;
import io.izzel.arclight.common.bridge.core.world.entity.player.PlayerBridge;
import io.izzel.arclight.common.bridge.core.world.inventory.LecternMenuBridge;
import io.izzel.arclight.common.bridge.core.world.inventory.LecternInventoryBridge;
import io.izzel.arclight.common.mod.mixins.annotation.CreateConstructor;
import io.izzel.arclight.common.mod.mixins.annotation.ShadowConstructor;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import io.izzel.arclight.mixin.Local;
import io.papermc.paper.event.player.PlayerLecternPageChangeEvent;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.LecternMenu;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.inventory.CraftInventoryLectern;
import org.bukkit.craftbukkit.inventory.view.CraftLecternView;
import org.bukkit.event.player.PlayerTakeLecternBookEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LecternMenu.class)
public abstract class LecternMenuMixin extends AbstractContainerMenuMixin implements LecternMenuBridge {

    // @formatter:off
    @Shadow @Final private Container lectern;
    // @formatter:on

    private CraftLecternView bukkitEntity;
    private Inventory playerInventory;

    @ShadowConstructor
    public void arclight$constructor(int i) {
        throw new RuntimeException();
    }

    @ShadowConstructor
    public void arclight$constructor(int i, Container inventory, ContainerData intArray) {
        throw new RuntimeException();
    }

    @CreateConstructor
    public void arclight$constructor(int i, Inventory playerInventory) {
        arclight$constructor(i);
        this.playerInventory = playerInventory;
    }

    @CreateConstructor
    public void arclight$constructor(int i, Container inventory, ContainerData intArray, Inventory playerInventory) {
        arclight$constructor(i, inventory, intArray);
        this.playerInventory = playerInventory;
    }

    @Decorate(method = "clickMenuButton(Lnet/minecraft/world/entity/player/Player;I)Z", require = 3, allow = 3,
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/inventory/LecternMenu;setData(II)V"))
    private void arclight$changePage(LecternMenu menu, int dataId, int newPage, @Local(ordinal = 0) Player player,
                                    @Local(ordinal = 0) int buttonId, @Local(ordinal = 1) int currentPage) throws Throwable {
        // Paper only emits this event for the arrows, not the page-jump buttons.
        if (buttonId == LecternMenu.BUTTON_PREV_PAGE || buttonId == LecternMenu.BUTTON_NEXT_PAGE) {
            CraftInventoryLectern inventory = (CraftInventoryLectern) getBukkitView().getTopInventory();
            var direction = buttonId == LecternMenu.BUTTON_PREV_PAGE
                ? PlayerLecternPageChangeEvent.PageChangeDirection.LEFT
                : PlayerLecternPageChangeEvent.PageChangeDirection.RIGHT;
            var event = new PlayerLecternPageChangeEvent((org.bukkit.entity.Player) ((PlayerBridge) player).bridge$getBukkitEntity(),
                inventory.getHolder(), inventory.getBook(), direction, currentPage, newPage);
            if (!event.callEvent()) {
                DecorationOps.cancel().invoke(false);
                return;
            }
            newPage = event.getNewPage();
        }
        DecorationOps.callsite().invoke(menu, dataId, newPage);
    }

    @Inject(method = "clickMenuButton", cancellable = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/Container;removeItemNoUpdate(I)Lnet/minecraft/world/item/ItemStack;"))
    public void arclight$takeBook(Player playerIn, int id, CallbackInfoReturnable<Boolean> cir) {
        PlayerTakeLecternBookEvent event = new PlayerTakeLecternBookEvent(((ServerPlayerBridge) this.playerInventory.player).bridge$getBukkitEntity(), ((CraftInventoryLectern) getBukkitView().getTopInventory()).getHolder());
        Bukkit.getServer().getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "stillValid", cancellable = true, at = @At("HEAD"))
    public void arclight$unreachable(Player playerIn, CallbackInfoReturnable<Boolean> cir) {
        if (this.lectern instanceof LecternInventoryBridge inventory && !inventory.bridge$getLectern().hasBook()) {
            cir.setReturnValue(false);
            return;
        }
        if (!bridge$isCheckReachable()) cir.setReturnValue(true);
    }

    @Override
    public CraftLecternView getBukkitView() {
        if (bukkitEntity != null) {
            return bukkitEntity;
        }
        CraftInventoryLectern inventory = new CraftInventoryLectern(this.lectern);
        bukkitEntity = new CraftLecternView(((ServerPlayerBridge) this.playerInventory.player).bridge$getBukkitEntity(), inventory, (LecternMenu) (Object) this);
        return bukkitEntity;
    }

    @Override
    public void bridge$setPlayerInventory(Inventory playerInventory) {
        this.playerInventory = playerInventory;
    }
}
