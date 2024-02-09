package io.izzel.arclight.neoforge.mixin.bukkit;

import io.izzel.arclight.i18n.ArclightConfig;
import io.izzel.arclight.neoforge.mod.permission.ArclightNeoForgePermissible;
import org.bukkit.craftbukkit.v.entity.CraftHumanEntity;
import org.bukkit.permissions.PermissibleBase;
import org.bukkit.permissions.ServerOperator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = CraftHumanEntity.class, remap = false)
public abstract class CraftHumanEntityMixin_NeoForge {

    @Redirect(method = "<init>", at = @At(value = "NEW", target = "(Lorg/bukkit/permissions/ServerOperator;)Lorg/bukkit/permissions/PermissibleBase;"))
    private PermissibleBase arclight$forge$forwardPerm(ServerOperator opable) {
        if (ArclightConfig.spec().getCompat().isForwardPermissionReverse()) {
            return new ArclightNeoForgePermissible(opable);
        } else {
            return new PermissibleBase(opable);
        }
    }
}
