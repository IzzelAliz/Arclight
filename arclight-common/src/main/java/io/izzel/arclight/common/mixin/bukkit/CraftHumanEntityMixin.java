package io.izzel.arclight.common.mixin.bukkit;

import io.izzel.arclight.common.mod.server.ArclightForgePermissible;
import io.izzel.arclight.common.mod.util.ArclightCaptures;
import io.izzel.arclight.i18n.ArclightConfig;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.bukkit.craftbukkit.v.CraftServer;
import org.bukkit.craftbukkit.v.entity.CraftEntity;
import org.bukkit.craftbukkit.v.entity.CraftHumanEntity;
import org.bukkit.craftbukkit.v.inventory.CraftInventoryPlayer;
import org.bukkit.inventory.InventoryView;
import org.bukkit.permissions.PermissibleBase;
import org.bukkit.permissions.ServerOperator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = CraftHumanEntity.class, remap = false)
public abstract class CraftHumanEntityMixin extends CraftEntity {

    // @formatter:off
    @Shadow private CraftInventoryPlayer inventory;
    @Shadow public abstract Player getHandle();
    // @formatter:on

    public CraftHumanEntityMixin(CraftServer server, Entity entity) {
        super(server, entity);
    }

    @Redirect(method = "<init>", at = @At(value = "NEW", target = "org/bukkit/permissions/PermissibleBase"))
    private PermissibleBase arclight$forwardPerm(ServerOperator opable) {
        if (ArclightConfig.spec().getCompat().isForwardPermissionReverse()) {
            return new ArclightForgePermissible(opable);
        } else {
            return new PermissibleBase(opable);
        }
    }

    @Inject(method = "getOpenInventory", at = @At("HEAD"))
    private void arclight$capturePlayer(CallbackInfoReturnable<InventoryView> cir) {
        ArclightCaptures.captureContainerOwner(this.getHandle());
    }

    @Inject(method = "getOpenInventory", at = @At("RETURN"))
    private void arclight$resetPlayer(CallbackInfoReturnable<InventoryView> cir) {
        ArclightCaptures.resetContainerOwner();
    }

    @Override
    public void setHandle(Entity entity) {
        super.setHandle(entity);
        this.inventory = new CraftInventoryPlayer(((Player) entity).getInventory());
    }
}
