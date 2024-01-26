package io.izzel.arclight.common.mixin.core.world.level.block;

import io.izzel.arclight.common.bridge.core.world.WorldBridge;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CropBlock.class)
public abstract class CropBlockMixin extends BlockMixin {

    @Redirect(method = "growCrops(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"))
    public boolean arclight$blockGrowGrow(Level world, BlockPos pos, BlockState newState, int flags) {
        return CraftEventFactory.handleBlockGrowEvent(world, pos, newState, flags);
    }

    @Redirect(method = "entityInside", require = 0, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/GameRules;getBoolean(Lnet/minecraft/world/level/GameRules$Key;)Z"))
    public boolean arclight$entityChangeBlock(GameRules instance, GameRules.Key<GameRules.BooleanValue> arg, BlockState state, Level world, BlockPos pos, Entity entity) {
        boolean result = ((WorldBridge) world).bridge$forge$mobGriefing(entity);
        return !CraftEventFactory.callEntityChangeBlockEvent(entity, pos, state, result);
    }

    @Redirect(method = "randomTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"))
    public boolean arclight$blockGrowTick(ServerLevel world, BlockPos pos, BlockState newState, int flags) {
        return CraftEventFactory.handleBlockGrowEvent(world, pos, newState, flags);
    }

    @Inject(method = "getGrowthSpeed", cancellable = true, at = @At("RETURN"))
    private static void arclight$spigotModifier(Block block, BlockGetter blockGetter, BlockPos pos, CallbackInfoReturnable<Float> cir) {
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
