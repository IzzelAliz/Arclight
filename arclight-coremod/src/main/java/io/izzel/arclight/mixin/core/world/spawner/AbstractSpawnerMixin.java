package io.izzel.arclight.mixin.core.world.spawner;

import io.izzel.arclight.bridge.world.WorldBridge;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.util.WeightedSpawnerEntity;
import net.minecraft.world.World;
import net.minecraft.world.spawner.AbstractSpawner;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(AbstractSpawner.class)
public abstract class AbstractSpawnerMixin {

    // @formatter:off
    @Shadow public abstract World getWorld();
    @Shadow @Final public List<WeightedSpawnerEntity> potentialSpawns;
    // @formatter:on

    @Inject(method = "func_221409_a", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;addEntity(Lnet/minecraft/entity/Entity;)Z"))
    public void arclight$spawnReason(Entity entityIn, CallbackInfo ci) {
        ((WorldBridge) this.getWorld()).bridge$pushAddEntityReason(CreatureSpawnEvent.SpawnReason.SPAWNER);
    }

    @Inject(method = "setEntityType", at = @At("RETURN"))
    public void arclight$clearMobs(EntityType<?> type, CallbackInfo ci) {
        this.potentialSpawns.clear();
    }
}
