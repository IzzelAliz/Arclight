package io.izzel.arclight.common.mixin.core.entity.ai.goal;

import io.izzel.arclight.common.bridge.entity.MobEntityBridge;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.ai.goal.BreakBlockGoal;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IWorldReader;
import net.minecraft.world.World;
import net.minecraft.world.chunk.IChunk;
import net.minecraftforge.event.ForgeEventFactory;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v.block.CraftBlock;
import org.bukkit.event.entity.EntityInteractEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
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

    @Redirect(method = "shouldExecute", at = @At(value = "INVOKE", target = "Lnet/minecraftforge/common/ForgeHooks;canEntityDestroy(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/entity/LivingEntity;)Z"))
    private boolean arclight$mobGriefing(World world, BlockPos pos, LivingEntity entity) {
        return ForgeEventFactory.getMobGriefingEvent(world, entity);
    }

    @Inject(method = "shouldMoveTo", cancellable = true, locals = LocalCapture.CAPTURE_FAILHARD, at = @At(value = "INVOKE", ordinal = 0, target = "Lnet/minecraft/world/chunk/IChunk;getBlockState(Lnet/minecraft/util/math/BlockPos;)Lnet/minecraft/block/BlockState;"))
    private void arclight$canDestroy(IWorldReader worldIn, BlockPos pos, CallbackInfoReturnable<Boolean> cir, IChunk chunk) {
        if (!chunk.getBlockState(pos).canEntityDestroy(worldIn, pos, entity)) {
            cir.setReturnValue(false);
        }
    }
}
