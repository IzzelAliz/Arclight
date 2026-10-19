package io.izzel.arclight.common.mixin.vanilla.world.entity;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import io.izzel.arclight.common.mod.util.ChorusTeleportContext;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(LivingEntity.class)
public abstract class LivingEntityChorusTeleportMixin_Vanilla {
    @WrapMethod(method = "randomTeleport(DDDZ)Z")
    private boolean arclight$chorusTeleportScope(double x, double y, double z, boolean showParticles, Operation<Boolean> original) {
        ChorusTeleportContext.Frame frame = ChorusTeleportContext.enter((LivingEntity) (Object) this);
        try {
            return original.call(x, y, z, showParticles);
        } finally {
            ChorusTeleportContext.exit(frame);
        }
    }
}
