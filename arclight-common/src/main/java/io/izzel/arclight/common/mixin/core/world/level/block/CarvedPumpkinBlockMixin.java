package io.izzel.arclight.common.mixin.core.world.level.block;

import io.izzel.arclight.common.bridge.core.entity.EntityBridge;
import io.izzel.arclight.common.bridge.core.world.WorldBridge;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CarvedPumpkinBlock;
import net.minecraft.world.level.block.state.pattern.BlockPattern;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CarvedPumpkinBlock.class)
public class CarvedPumpkinBlockMixin {

    @Redirect(method = "spawnGolemInWorld", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/CarvedPumpkinBlock;clearPatternBlocks(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/level/block/state/pattern/BlockPattern$BlockPatternMatch;)V"))
    private static void arclight$clearLater(Level level, BlockPattern.BlockPatternMatch match, Level p_249110_, BlockPattern.BlockPatternMatch p_251293_, Entity entity) {
        ((WorldBridge) level).bridge$pushAddEntityReason((entity.getType() == EntityType.SNOW_GOLEM) ? CreatureSpawnEvent.SpawnReason.BUILD_SNOWMAN : CreatureSpawnEvent.SpawnReason.BUILD_IRONGOLEM);
    }

    @Inject(method = "spawnGolemInWorld", cancellable = true, at = @At(value = "INVOKE", shift = At.Shift.AFTER, target = "Lnet/minecraft/world/level/Level;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"))
    private static void arclight$clearPattern(Level p_249110_, BlockPattern.BlockPatternMatch p_251293_, Entity entity, BlockPos p_251189_, CallbackInfo ci) {
        if (!((EntityBridge) entity).bridge$isValid()) {
            ci.cancel();
        } else {
            CarvedPumpkinBlock.clearPatternBlocks(p_249110_, p_251293_);
        }
    }
}
