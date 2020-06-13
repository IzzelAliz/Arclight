package io.izzel.arclight.common.mixin.core.command.impl;

import io.izzel.arclight.common.bridge.world.server.ServerWorldBridge;
import net.minecraft.command.CommandSource;
import net.minecraft.command.impl.SummonCommand;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.Vec3d;
import org.bukkit.event.weather.LightningStrikeEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SummonCommand.class)
public class SummonCommandMixin {

    @Inject(method = "summonEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/server/ServerWorld;addLightningBolt(Lnet/minecraft/entity/effect/LightningBoltEntity;)V"))
    private static void arclight$strikeReason(CommandSource source, ResourceLocation type, Vec3d pos, CompoundNBT nbt, boolean randomizeProperties, CallbackInfoReturnable<Integer> cir) {
        ((ServerWorldBridge) source.getWorld()).bridge$pushStrikeLightningCause(LightningStrikeEvent.Cause.COMMAND);
    }
}
