package io.izzel.arclight.common.mixin.core.world.level.block;

import io.izzel.arclight.common.bridge.core.server.level.ServerPlayerBridge;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageSources;
import net.minecraft.world.phys.Vec3;
import org.bukkit.craftbukkit.block.CraftBlock;
import io.izzel.arclight.common.bridge.core.world.damagesource.DamageSourceBridge;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import io.izzel.arclight.mixin.Local;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RespawnAnchorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.bukkit.event.player.PlayerSpawnChangeEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RespawnAnchorBlock.class)
public class RespawnAnchorBlockMixin {

    @Inject(method = "useWithoutItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;setRespawnPosition(Lnet/minecraft/server/level/ServerPlayer$RespawnConfig;Z)V"))
    private void arclight$cause(BlockState blockState, Level level, BlockPos blockPos, Player player, BlockHitResult blockHitResult, CallbackInfoReturnable<InteractionResult> cir) {
        ((ServerPlayerBridge) player).bridge$pushChangeSpawnCause(PlayerSpawnChangeEvent.Cause.RESPAWN_ANCHOR);
    }
    @Decorate(method = "explode", inject = true, at = @At("HEAD"))
    private void arclight$captureAnchor(BlockState state, ServerLevel level, BlockPos pos, @Local(allocate = "arclightAnchorSnapshot") org.bukkit.block.BlockState snapshot) throws Throwable {
        snapshot = CraftBlock.at(level, pos).getState();
        DecorationOps.blackhole().invoke(snapshot);
    }

    @Decorate(method = "explode", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/damagesource/DamageSources;badRespawnPointExplosion(Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/damagesource/DamageSource;"))
    private DamageSource arclight$anchorDamageSource(DamageSources sources, Vec3 position, @Local(allocate = "arclightAnchorSnapshot") org.bukkit.block.BlockState snapshot) throws Throwable {
        var source = (DamageSource) DecorationOps.callsite().invoke(sources, position);
        return ((DamageSourceBridge) source).bridge$directBlockState(snapshot);
    }

}
