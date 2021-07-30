package io.izzel.arclight.common.mixin.core.server.commands;

import io.izzel.arclight.common.bridge.core.world.server.ServerWorldBridge;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.commands.SummonCommand;
import net.minecraft.world.phys.Vec3;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SummonCommand.class)
public class SummonCommandMixin {

    @Inject(method = "spawnEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;tryAddFreshEntityWithPassengers(Lnet/minecraft/world/entity/Entity;)Z"))
    private static void arclight$summonReason(CommandSourceStack source, ResourceLocation type, Vec3 pos, CompoundTag nbt, boolean randomizeProperties, CallbackInfoReturnable<Integer> cir) {
        ((ServerWorldBridge) source.getLevel()).bridge$pushAddEntityReason(CreatureSpawnEvent.SpawnReason.COMMAND);
    }
}
