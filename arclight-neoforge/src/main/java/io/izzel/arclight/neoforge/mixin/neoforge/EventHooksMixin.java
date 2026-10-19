package io.izzel.arclight.neoforge.mixin.neoforge;

import io.izzel.arclight.common.bridge.core.entity.EntityBridge;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import org.bukkit.event.entity.EntityRemoveEvent;
import io.izzel.arclight.common.mod.util.ArclightCaptures;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.common.util.BlockSnapshot;
import net.neoforged.neoforge.event.EventHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(EventHooks.class)
public abstract class EventHooksMixin {
    @Inject(method = "onBlockPlace", remap = false, at = @At("HEAD"))
    private static void arclight$captureDirection(Entity entity, BlockSnapshot blockSnapshot, Direction direction, CallbackInfoReturnable<Boolean> cir) {
        ArclightCaptures.capturePlaceEventDirection(direction);
    }

    @Inject(method = "onMultiBlockPlace", remap = false, at = @At("HEAD"))
    private static void arclight$captureDirection(Entity entity, List<BlockSnapshot> blockSnapshots, Direction direction, CallbackInfoReturnable<Boolean> cir) {
        ArclightCaptures.capturePlaceEventDirection(direction);
    }
    @Inject(method = "checkMobDespawn(Lnet/minecraft/world/entity/Mob;)Z", remap = false,
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Mob;discard()V", remap = false))
    private static void arclight$witherDespawnCause(Mob mob, CallbackInfoReturnable<Boolean> cir) {
        if (mob instanceof WitherBoss) {
            ((EntityBridge) mob).bridge$pushEntityRemoveCause(EntityRemoveEvent.Cause.DESPAWN);
        }
    }

}
