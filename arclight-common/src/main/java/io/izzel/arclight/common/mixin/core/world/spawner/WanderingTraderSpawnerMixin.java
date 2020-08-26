package io.izzel.arclight.common.mixin.core.world.spawner;

import io.izzel.arclight.common.bridge.world.WorldBridge;
import net.minecraft.entity.merchant.villager.WanderingTraderEntity;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.spawner.WanderingTraderSpawner;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(WanderingTraderSpawner.class)
public class WanderingTraderSpawnerMixin {

    @Inject(method = "func_234562_a_", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/EntityType;spawn(Lnet/minecraft/world/server/ServerWorld;Lnet/minecraft/nbt/CompoundNBT;Lnet/minecraft/util/text/ITextComponent;Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/entity/SpawnReason;ZZ)Lnet/minecraft/entity/Entity;"))
    public void arclight$spawnReason1(ServerWorld serverWorld, CallbackInfoReturnable<Boolean> cir) {
        ((WorldBridge) serverWorld).bridge$pushAddEntityReason(CreatureSpawnEvent.SpawnReason.NATURAL);
    }

    @Inject(method = "func_242373_a", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/EntityType;spawn(Lnet/minecraft/world/server/ServerWorld;Lnet/minecraft/nbt/CompoundNBT;Lnet/minecraft/util/text/ITextComponent;Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/entity/SpawnReason;ZZ)Lnet/minecraft/entity/Entity;"))
    public void arclight$spawnReason2(ServerWorld serverWorld, WanderingTraderEntity p_242373_2_, int p_242373_3_, CallbackInfo ci) {
        ((WorldBridge) serverWorld).bridge$pushAddEntityReason(CreatureSpawnEvent.SpawnReason.NATURAL);
    }
}
