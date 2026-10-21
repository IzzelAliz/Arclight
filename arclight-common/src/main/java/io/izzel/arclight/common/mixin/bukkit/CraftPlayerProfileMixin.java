package io.izzel.arclight.common.mixin.bukkit;

import org.bukkit.craftbukkit.v.profile.CraftPlayerProfile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.Objects;
import java.util.UUID;

@Mixin(value = CraftPlayerProfile.class, remap = false)
public abstract class CraftPlayerProfileMixin {

    private static final UUID NIL_UUID = new UUID(0L, 0L);

    @ModifyVariable(method = "<init>(Ljava/util/UUID;Ljava/lang/String;Z)V", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private static UUID arclight$fixUniqueId(UUID uniqueId, UUID u, String name, boolean applyPreconditions) {
        if ((uniqueId == null || Objects.equals(uniqueId, NIL_UUID)) && (name == null || name.isBlank())) {
            return UUID.randomUUID();
        }
        return uniqueId;
    }
}
