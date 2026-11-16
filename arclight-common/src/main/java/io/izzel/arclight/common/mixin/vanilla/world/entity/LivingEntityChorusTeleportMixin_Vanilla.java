package io.izzel.arclight.common.mixin.vanilla.world.entity;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import io.izzel.arclight.common.mod.util.ArclightCaptures;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(LivingEntity.class)
public abstract class LivingEntityChorusTeleportMixin_Vanilla {
    @WrapMethod(method = "randomTeleport(DDDZLjava/util/function/Predicate;)Z")
    private boolean arclight$chorusTeleportScope(double x, double y, double z, boolean showParticles, java.util.function.Predicate<net.minecraft.world.level.block.state.BlockState> isInvalidPosition, Operation<Boolean> original) {
        var previous = ArclightCaptures.chorusActive;
        var pending = ArclightCaptures.chorusPending;
        ArclightCaptures.chorusActive = pending != null && pending.getFirst() == (Object) this ? pending : null;
        if (ArclightCaptures.chorusActive != null) ArclightCaptures.chorusPending = null;
        try {
            return original.call(x, y, z, showParticles, isInvalidPosition);
        } finally {
            ArclightCaptures.chorusActive = previous;
        }
    }
}
