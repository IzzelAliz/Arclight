package io.izzel.arclight.common.mixin.core.command.impl;

import io.izzel.arclight.common.bridge.world.server.ServerWorldBridge;
import net.minecraft.command.CommandSource;
import net.minecraft.command.impl.SummonCommand;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.Vector3d;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SummonCommand.class)
public class SummonCommandMixin {

    @Inject(method = "summonEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/server/ServerWorld;addEntityAndUniquePassengers(Lnet/minecraft/entity/Entity;)Z"))
    private static void arclight$summonReason(CommandSource source, ResourceLocation type, Vector3d pos, CompoundNBT nbt, boolean randomizeProperties, CallbackInfoReturnable<Integer> cir) {
        ((ServerWorldBridge) source.getWorld()).bridge$pushAddEntityReason(CreatureSpawnEvent.SpawnReason.COMMAND);
    }
}
