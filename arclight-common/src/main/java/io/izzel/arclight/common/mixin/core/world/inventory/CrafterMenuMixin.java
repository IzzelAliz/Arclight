package io.izzel.arclight.common.mixin.core.world.inventory;

import io.izzel.arclight.common.bridge.core.entity.player.ServerPlayerEntityBridge;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.CrafterMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.ResultContainer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.bukkit.craftbukkit.v.inventory.CraftInventoryCrafter;
import org.bukkit.craftbukkit.v.inventory.CraftInventoryView;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CrafterMenu.class)
public abstract class CrafterMenuMixin extends AbstractContainerMenuMixin {

    @Shadow @Final private CraftingContainer container;
    @Shadow @Final private ResultContainer resultContainer;
    @Shadow @Final private Player player;

    private CraftInventoryView bukkitEntity;

    @Override
    public CraftInventoryView getBukkitView() {
        if (bukkitEntity != null) {
            return bukkitEntity;
        }

        CraftInventoryCrafter inventory = new CraftInventoryCrafter(this.container, this.resultContainer);
        bukkitEntity = new CraftInventoryView(((ServerPlayerEntityBridge) this.player).bridge$getBukkitEntity(), inventory, (CrafterMenu) (Object) this);
        return bukkitEntity;
    }

    @Inject(method = "stillValid", cancellable = true, at = @At("HEAD"))
    public void arclight$unreachable(Player playerIn, CallbackInfoReturnable<Boolean> cir) {
        if (!bridge$isCheckReachable()) cir.setReturnValue(true);
    }
}
