package io.izzel.arclight.common.mixin.core.world.level.dimension.end;

import io.izzel.arclight.common.bridge.core.entity.EntityBridge;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.level.dimension.end.EnderDragonFight;
import org.bukkit.event.entity.EntityRemoveEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(targets = "net/minecraft/world/level/dimension/end/DragonRespawnAnimation$4")
public class DragonRespawnAnimation_4_Mixin {

    @Inject(method = "tick", at = @At("HEAD"))
    private void arclight$explode(ServerLevel serverLevel, EnderDragonFight endDragonFight, List<EndCrystal> list, int i, BlockPos blockPos, CallbackInfo ci) {
        if (i >= 100) {
            for (var endCrystal : list) {
                ((EntityBridge) endCrystal).bridge$pushEntityRemoveCause(EntityRemoveEvent.Cause.EXPLODE);
            }
        }
    }
}
