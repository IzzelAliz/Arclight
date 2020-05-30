package io.izzel.arclight.common.mixin.core.entity.ai.goal;

import io.izzel.arclight.common.bridge.entity.MobEntityBridge;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.ai.goal.BreakBlockGoal;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v.block.CraftBlock;
import org.bukkit.event.entity.EntityInteractEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(BreakBlockGoal.class)
public class BreakBlockGoalMixin {

    // @formatter:off
    @Shadow @Final private MobEntity entity;
    // @formatter:on

    @Inject(method = "tick", cancellable = true, locals = LocalCapture.CAPTURE_FAILHARD,
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;removeBlock(Lnet/minecraft/util/math/BlockPos;Z)Z"))
    public void arclight$removeBlock(CallbackInfo ci, World world, BlockPos pos, BlockPos pos1) {
        EntityInteractEvent event = new EntityInteractEvent(((MobEntityBridge) this.entity).bridge$getBukkitEntity(), CraftBlock.at(world, pos1));
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            ci.cancel();
        }
    }
}
