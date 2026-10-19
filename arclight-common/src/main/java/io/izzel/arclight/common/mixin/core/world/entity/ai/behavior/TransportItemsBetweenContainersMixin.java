package io.izzel.arclight.common.mixin.core.world.entity.ai.behavior;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalDoubleRef;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.behavior.TransportItemsBetweenContainers;
import net.minecraft.world.entity.ai.behavior.TransportItemsBetweenContainers.TransportItemTarget;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.block.CraftBlock;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(TransportItemsBetweenContainers.class)
public abstract class TransportItemsBetweenContainersMixin {

    // The second IFNULL is the candidate gate, after the native closer-distance and validity checks.
    // Its taken (null) edge skips this injection; only a better, valid candidate reaches the handler.
    @Inject(method = "getTransportTarget", at = @At(value = "JUMP", opcode = 198, ordinal = 1, shift = At.Shift.AFTER), cancellable = true)
    private void arclight$candidateTarget(ServerLevel level, PathfinderMob body,
            CallbackInfoReturnable<Optional<TransportItemTarget>> cir,
            @Local(ordinal = 0) TransportItemTarget best,
            @Local(ordinal = 1) LocalRef<TransportItemTarget> candidate,
            @Local(ordinal = 0) double closestDistance,
            @Local(ordinal = 1) LocalDoubleRef distance) {
        var event = CraftEventFactory.callEntityTargetBlockEvent(body, level, candidate.get().pos());
        if (event.isCancelled()) {
            cir.setReturnValue(Optional.empty());
            return;
        }
        Block newTarget = event.getTarget();
        if (newTarget == null) {
            // Equivalent to CB's continue: the two native stores must preserve the previous best
            // and distance. Do not confuse this with a non-null edit whose rebuild returns null.
            candidate.set(best);
            distance.set(closestDistance);
            return;
        }
        // CB intentionally rebuilds in the original level, without rerunning the selection gates.
        // Even a null rebuild replaces the previous best and writes the ORIGINAL candidate distance.
        candidate.set(TransportItemTarget.tryCreatePossibleTarget(((CraftBlock) newTarget).getPosition(), level));
    }
}
