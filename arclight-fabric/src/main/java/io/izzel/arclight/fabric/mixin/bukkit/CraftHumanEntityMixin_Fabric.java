package io.izzel.arclight.fabric.mixin.bukkit;

import io.izzel.arclight.fabric.mod.permission.ArclightFabricPermissible;
import io.izzel.arclight.i18n.ArclightConfig;
import org.bukkit.craftbukkit.entity.CraftHumanEntity;
import org.bukkit.permissions.PermissibleBase;
import org.bukkit.permissions.ServerOperator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = CraftHumanEntity.class, remap = false)
public abstract class CraftHumanEntityMixin_Fabric {

    @Redirect(method = "<init>", at = @At(value = "NEW", target = "(Lorg/bukkit/permissions/ServerOperator;)Lorg/bukkit/permissions/PermissibleBase;"))
    private PermissibleBase arclight$forge$forwardPerm(ServerOperator opable) {
        if (ArclightConfig.spec().getCompat().isForwardPermissionReverse()) {
            return new ArclightFabricPermissible(opable);
        } else {
            return new PermissibleBase(opable);
        }
    }
}
