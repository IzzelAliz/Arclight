package io.izzel.arclight.common.mixin.core.world.level.gameevent.vibrations;

import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gameevent.vibrations.VibrationSystem;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v.CraftGameEvent;
import org.bukkit.craftbukkit.v.block.CraftBlock;
import org.bukkit.event.block.BlockReceiveGameEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(VibrationSystem.Listener.class)
public abstract class VibrationListenerMixin {

    @Decorate(method = "handleGameEvent", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/gameevent/vibrations/VibrationSystem$User;canReceiveVibration(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Holder;Lnet/minecraft/world/level/gameevent/GameEvent$Context;)Z"))
    private boolean arclight$gameEvent(VibrationSystem.User instance, ServerLevel serverLevel, BlockPos pos, Holder<GameEvent> gameEventHolder, GameEvent.Context context) throws Throwable {
        var defaultCancel = !(boolean) DecorationOps.callsite().invoke(instance, serverLevel, pos, gameEventHolder, context);
        Entity entity = context.sourceEntity();
        BlockReceiveGameEvent event = new BlockReceiveGameEvent(CraftGameEvent.minecraftToBukkit(gameEventHolder.value()), CraftBlock.at(serverLevel, pos), (entity == null) ? null : entity.bridge$getBukkitEntity());
        event.setCancelled(defaultCancel);
        Bukkit.getPluginManager().callEvent(event);
        return !event.isCancelled();
    }
}
