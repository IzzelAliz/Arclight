package io.izzel.arclight.common.mixin.core.village;

import io.izzel.arclight.common.bridge.world.server.ServerWorldBridge;
import net.minecraft.village.VillageSiege;
import net.minecraft.world.server.ServerWorld;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(VillageSiege.class)
public class VillageSiegeMixin {

    @Inject(method = "spawnZombie", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/server/ServerWorld;func_242417_l(Lnet/minecraft/entity/Entity;)V"))
    public void arclight$addEntityReason(ServerWorld world, CallbackInfo ci) {
        ((ServerWorldBridge) world).bridge$pushAddEntityReason(CreatureSpawnEvent.SpawnReason.VILLAGE_INVASION);
    }
}
