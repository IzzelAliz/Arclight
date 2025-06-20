package io.izzel.arclight.common.mixin.bukkit.event;

import org.bukkit.Bukkit;
import org.bukkit.event.world.AsyncStructureSpawnEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(value = AsyncStructureSpawnEvent.class, remap = false)
public class AsyncStructureSpawnEventMixin {

    @ModifyArg(method = "<init>", at = @At(value = "INVOKE", target = "Lorg/bukkit/event/world/WorldEvent;<init>(Lorg/bukkit/World;Z)V"))
    private static boolean arclight$async(boolean async) {
        return !Bukkit.isPrimaryThread();
    }
}
