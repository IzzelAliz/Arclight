package io.izzel.arclight.neoforge.mixin.core.world.entity;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import io.izzel.arclight.common.mod.util.ChorusTeleportContext;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(LivingEntity.class)
public abstract class LivingEntityChorusTeleportMixin_NeoForge {
    // Scope only the body. The four-argument compatibility entry delegates here with ItemStack.EMPTY.
    @WrapMethod(method = "randomTeleport(DDDZLnet/minecraft/world/item/ItemStack;)Z")
    private boolean arclight$chorusTeleportScope(double x, double y, double z, boolean showParticles,
                                                 ItemStack consumedStack, Operation<Boolean> original) {
        ChorusTeleportContext.Frame frame = ChorusTeleportContext.enter((LivingEntity) (Object) this);
        try {
            return original.call(x, y, z, showParticles, consumedStack);
        } finally {
            ChorusTeleportContext.exit(frame);
        }
    }
}
