package io.izzel.arclight.common.mixin.core.world.level.block;

import io.izzel.arclight.common.bridge.core.entity.EntityBridge;
import io.izzel.arclight.common.bridge.core.world.WorldBridge;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.dimension.LevelStem;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v.block.CraftBlock;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.bukkit.event.entity.EntityCombustByBlockEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(BaseFireBlock.class)
public class BaseFireBlockMixin {

    // fireExtinguished implemented per class

    @Redirect(method = "entityInside", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;setSecondsOnFire(I)V"))
    private void arclight$onFire(Entity instance, int seconds, BlockState state, Level level, BlockPos pos) {
        var event = new EntityCombustByBlockEvent(CraftBlock.at(level, pos), ((EntityBridge) instance).bridge$getBukkitEntity(), seconds);
        Bukkit.getPluginManager().callEvent(event);

        if (!event.isCancelled()) {
            ((EntityBridge) instance).bridge$setOnFire(event.getDuration(), false);
        }
    }

    @Redirect(method = "onPlace", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;removeBlock(Lnet/minecraft/core/BlockPos;Z)Z"))
    public boolean arclight$extinguish2(Level world, BlockPos pos, boolean isMoving) {
        if (!CraftEventFactory.callBlockFadeEvent(world, pos, Blocks.AIR.defaultBlockState()).isCancelled()) {
            world.removeBlock(pos, isMoving);
        }
        return false;
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    private static boolean inPortalDimension(Level level) {
        var typeKey = ((WorldBridge) level).bridge$getTypeKey();
        return typeKey == LevelStem.NETHER || typeKey == LevelStem.OVERWORLD;
    }
}
