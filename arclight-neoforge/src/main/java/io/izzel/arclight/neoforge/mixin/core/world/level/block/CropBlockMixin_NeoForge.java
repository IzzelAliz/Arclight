package io.izzel.arclight.neoforge.mixin.core.world.level.block;

import io.izzel.arclight.common.bridge.core.world.level.WorldBridge;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.EventHooks;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CropBlock.class)
public abstract class CropBlockMixin_NeoForge {

    @Redirect(method = "entityInside", at = @At(value = "INVOKE", remap = false, target = "Lnet/neoforged/neoforge/event/EventHooks;canEntityGrief(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/Entity;)Z"))
    public boolean arclight$entityChangeBlock(Level world, Entity entity, BlockState state, Level worldIn, BlockPos pos) {
        boolean result = EventHooks.canEntityGrief(world, entity);
        return !CraftEventFactory.callEntityChangeBlockEvent(entity, pos, state, result);
    }

    @Inject(method = "getGrowthSpeed", cancellable = true, at = @At("RETURN"))
    private static void arclight$spigotModifier(BlockState state, BlockGetter blockGetter, BlockPos pos, CallbackInfoReturnable<Float> cir) {
        var block = state.getBlock();
        if (blockGetter instanceof WorldBridge bridge) {
            int modifier;
            if (block == Blocks.BEETROOTS) {
                modifier = bridge.bridge$spigotConfig().beetrootModifier;
            } else if (block == Blocks.CARROTS) {
                modifier = bridge.bridge$spigotConfig().carrotModifier;
            } else if (block == Blocks.POTATOES) {
                modifier = bridge.bridge$spigotConfig().potatoModifier;
            } else {
                modifier = bridge.bridge$spigotConfig().wheatModifier;
            }
            var f = cir.getReturnValueF();
            f /= (100F / modifier);
            cir.setReturnValue(f);
        }
    }
}
