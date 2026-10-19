package io.izzel.arclight.common.mixin.core.world.level.dimension.end;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import org.bukkit.event.entity.EntityRemoveEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(targets = "net/minecraft/world/level/dimension/end/DragonRespawnStage$4")
public class DragonRespawnAnimation_4_Mixin {

    @WrapOperation(method = "tick(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/level/dimension/end/EnderDragonFight;Ljava/util/List;I)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/boss/enderdragon/EndCrystal;discard()V"))
    private void arclight$explode(EndCrystal crystal, Operation<Void> original) {
        crystal.bridge().bridge$pushEntityRemoveCause(EntityRemoveEvent.Cause.EXPLODE);
        original.call(crystal);
    }
}
