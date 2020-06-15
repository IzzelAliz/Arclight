package io.izzel.arclight.common.mixin.core.entity.ai.goal;

import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ai.goal.EatGrassGoal;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.event.ForgeEventFactory;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(EatGrassGoal.class)
public class EatGrassGoalMixin {

    private transient BlockPos arclight$pos;

    @Inject(method = "tick", locals = LocalCapture.CAPTURE_FAILHARD, at = @At(value = "INVOKE", ordinal = 0, remap = false, target = "Lnet/minecraftforge/event/ForgeEventFactory;getMobGriefingEvent(Lnet/minecraft/world/World;Lnet/minecraft/entity/Entity;)Z"))
    public void arclight$capturePos1(CallbackInfo ci, BlockPos pos) {
        arclight$pos = pos;
    }

    @Inject(method = "tick", locals = LocalCapture.CAPTURE_FAILHARD, at = @At(value = "INVOKE", ordinal = 1, remap = false, target = "Lnet/minecraftforge/event/ForgeEventFactory;getMobGriefingEvent(Lnet/minecraft/world/World;Lnet/minecraft/entity/Entity;)Z"))
    public void arclight$capturePos2(CallbackInfo ci, BlockPos pos) {
        arclight$pos = pos.down();
    }

    @Redirect(method = "tick", at = @At(value = "INVOKE", remap = false, target = "Lnet/minecraftforge/event/ForgeEventFactory;getMobGriefingEvent(Lnet/minecraft/world/World;Lnet/minecraft/entity/Entity;)Z"))
    public boolean arclight$entityChangeBlock(World world, Entity entity) {
        boolean b = ForgeEventFactory.getMobGriefingEvent(world, entity);
        EntityChangeBlockEvent event = CraftEventFactory.callEntityChangeBlockEvent(entity, arclight$pos, Blocks.AIR.getDefaultState(), !b);
        arclight$pos = null;
        return !event.isCancelled();
    }
}
