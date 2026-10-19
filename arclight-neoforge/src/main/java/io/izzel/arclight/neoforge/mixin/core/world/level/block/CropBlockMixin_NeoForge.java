package io.izzel.arclight.neoforge.mixin.core.world.level.block;

import io.izzel.arclight.common.bridge.core.world.level.WorldBridge;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.TriState;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import io.izzel.arclight.mixin.Local;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.StemBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.EventHooks;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CropBlock.class)
public abstract class CropBlockMixin_NeoForge {

    @WrapOperation(method = "randomTick(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/util/RandomSource;)V",
        at = @At(value = "INVOKE", remap = false, target = "Lnet/neoforged/neoforge/common/CommonHooks;fireCropGrowPost(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)V"),
        require = 1, expect = 1, allow = 1)
    private void arclight$cropGrowPostAfterBukkitVeto(Level level, BlockPos pos, BlockState state, Operation<Void> original,
            @Share(value = "cancelled", namespace = "arclight:crop") LocalBooleanRef cancelled) {
        if (!cancelled.get()) {
            original.call(level, pos, state);
        }
    }

    @Redirect(method = "entityInside(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/entity/InsideBlockEffectApplier;Z)V", at = @At(value = "INVOKE", remap = false, target = "Lnet/neoforged/neoforge/event/EventHooks;canEntityGrief(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/Entity;)Z"))
    public boolean arclight$entityChangeBlock(ServerLevel world, Entity entity, BlockState state, Level worldIn, BlockPos pos, Entity entityIn, InsideBlockEffectApplier effectApplier, boolean steppingCarefully) {
        return CraftEventFactory.callEntityChangeBlockEvent(entity, pos, Blocks.AIR.defaultBlockState(), !EventHooks.canEntityGrief(world, entity));
    }

    @Decorate(method = "getGrowthSpeed", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;canSustainPlant(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;Lnet/minecraft/world/level/block/state/BlockState;)Lnet/minecraft/util/TriState;", remap = false))
    private static TriState arclight$cropSoilArgument(BlockState soil, BlockGetter level, BlockPos soilPos, Direction face, BlockState ignored, @Local(ordinal = 0) BlockState crop) throws Throwable {
        return (TriState) DecorationOps.callsite().invoke(soil, level, soilPos, face, crop);
    }

    @Inject(method = "getGrowthSpeed", cancellable = true, at = @At("RETURN"))
    private static void arclight$spigotModifier(BlockState state, BlockGetter blockGetter, BlockPos pos, CallbackInfoReturnable<Float> cir) {
        var block = state.getBlock();
        // StemBlock calls getGrowthSpeed too; its per-world rate belongs to its
        // randomTick gate, not the wheat fallback (which would double-scale it).
        if (!(block instanceof StemBlock) && blockGetter instanceof WorldBridge bridge) {
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
