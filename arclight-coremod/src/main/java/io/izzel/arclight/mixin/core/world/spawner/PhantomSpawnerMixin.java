package io.izzel.arclight.mixin.core.world.spawner;

import io.izzel.arclight.bridge.world.WorldBridge;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.spawner.PhantomSpawner;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PhantomSpawner.class)
public class PhantomSpawnerMixin {

    @Inject(method = "func_203232_a", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/server/ServerWorld;addEntity(Lnet/minecraft/entity/Entity;)Z"))
    public void arclight$spawnReason(ServerWorld worldIn, boolean spawnHostileMobs, boolean spawnPeacefulMobs, CallbackInfoReturnable<Integer> cir) {
        ((WorldBridge) worldIn).bridge$pushAddEntityReason(CreatureSpawnEvent.SpawnReason.NATURAL);
    }
}
