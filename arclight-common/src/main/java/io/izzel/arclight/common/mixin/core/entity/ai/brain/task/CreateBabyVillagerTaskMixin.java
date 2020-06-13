package io.izzel.arclight.common.mixin.core.entity.ai.brain.task;

import io.izzel.arclight.common.bridge.world.WorldBridge;
import net.minecraft.entity.ai.brain.task.CreateBabyVillagerTask;
import net.minecraft.entity.merchant.villager.VillagerEntity;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.Optional;

@Mixin(CreateBabyVillagerTask.class)
public class CreateBabyVillagerTaskMixin {

    @Inject(method = "func_220480_a", cancellable = true, locals = LocalCapture.CAPTURE_FAILHARD,
        at = @At(value = "JUMP", ordinal = 0, opcode = Opcodes.IFNONNULL))
    private void arclight$entityBreed(VillagerEntity lona, VillagerEntity anonymous, CallbackInfoReturnable<Optional<VillagerEntity>> cir, VillagerEntity child) {
        if (CraftEventFactory.callEntityBreedEvent(child, lona, anonymous, null, null, 0).isCancelled()) {
            cir.setReturnValue(Optional.empty());
        } else if (child != null) {
            ((WorldBridge) lona.world).bridge$pushAddEntityReason(CreatureSpawnEvent.SpawnReason.BREEDING);
        }
    }
}
