package io.izzel.arclight.common.mixin.core.world.level.block;

import io.izzel.arclight.common.bridge.core.world.damagesource.DamageSourceBridge;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import io.izzel.arclight.mixin.Local;
import net.minecraft.core.BlockPos;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageSources;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.bukkit.craftbukkit.block.CraftBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(BedBlock.class)
public abstract class BedBlockMixin {

    @Decorate(method = "destroyOnUse", inject = true, require = 1, allow = 1, at = @At("HEAD"))
    private void arclight$captureBed(BlockState state, Level level, BlockPos pos, Player player,
            @Local(allocate = "arclightBedSnapshot") org.bukkit.block.BlockState snapshot) throws Throwable {
        snapshot = CraftBlock.at(level, pos).getState();
        DecorationOps.blackhole().invoke(snapshot);
    }

    @Decorate(method = "destroyOnUse", require = 1, allow = 1,
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/damagesource/DamageSources;badRespawnPointExplosion(Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/damagesource/DamageSource;"))
    private DamageSource arclight$bedDamageSource(DamageSources sources, Vec3 pos,
            @Local(allocate = "arclightBedSnapshot") org.bukkit.block.BlockState snapshot) throws Throwable {
        DamageSource source = (DamageSource) DecorationOps.callsite().invoke(sources, pos);
        return ((DamageSourceBridge) source).bridge$directBlockState(snapshot);
    }
}
