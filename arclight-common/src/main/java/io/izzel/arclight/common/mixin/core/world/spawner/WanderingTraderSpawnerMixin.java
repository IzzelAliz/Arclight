package io.izzel.arclight.common.mixin.core.world.spawner;

import io.izzel.arclight.common.bridge.world.WorldBridge;
import net.minecraft.entity.merchant.villager.WanderingTraderEntity;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.spawner.WanderingTraderSpawner;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(WanderingTraderSpawner.class)
public class WanderingTraderSpawnerMixin {

    // @formatter:off
    @Shadow @Final private ServerWorld world;
    // @formatter:on

    @Inject(method = "func_221245_b", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/EntityType;spawn(Lnet/minecraft/world/World;Lnet/minecraft/nbt/CompoundNBT;Lnet/minecraft/util/text/ITextComponent;Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/entity/SpawnReason;ZZ)Lnet/minecraft/entity/Entity;"))
    public void arclight$spawnReason1(CallbackInfoReturnable<Boolean> cir) {
        ((WorldBridge) this.world).bridge$pushAddEntityReason(CreatureSpawnEvent.SpawnReason.NATURAL);
    }

    @Inject(method = "func_221243_a", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/EntityType;spawn(Lnet/minecraft/world/World;Lnet/minecraft/nbt/CompoundNBT;Lnet/minecraft/util/text/ITextComponent;Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/entity/SpawnReason;ZZ)Lnet/minecraft/entity/Entity;"))
    public void arclight$spawnReason2(WanderingTraderEntity p_221243_1_, int p_221243_2_, CallbackInfo ci) {
        ((WorldBridge) this.world).bridge$pushAddEntityReason(CreatureSpawnEvent.SpawnReason.NATURAL);
    }
}
