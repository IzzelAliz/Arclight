package io.izzel.arclight.common.mixin.core.world.level.block;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RedStoneOreBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.block.CraftBlock;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityInteractEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RedStoneOreBlock.class)
public abstract class RedStoneOreBlockMixin {

    // @formatter:off
    @Shadow private static void interact(BlockState state, Level world, BlockPos pos) { }
    // @formatter:on

    // Scoped to the actual interact invocation, including third-party Operation wrappers.
    private static final ThreadLocal<Entity> arclight$entity = new ThreadLocal<>();

    @WrapOperation(method = "attack", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/RedStoneOreBlock;interact(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)V"))
    private void arclight$interact1(BlockState state, Level world, BlockPos pos, Operation<Void> original,
                                    BlockState hostState, Level hostWorld, BlockPos hostPos, Player player) {
        arclight$withEntity(state, world, pos, player, original);
    }

    @WrapOperation(method = "stepOn", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/RedStoneOreBlock;interact(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)V"))
    private void arclight$entityInteract(BlockState state, Level world, BlockPos pos, Operation<Void> original,
                                        Level hostWorld, BlockPos hostPos, BlockState hostState, Entity entity) {
        if (entity instanceof Player player) {
            PlayerInteractEvent event = CraftEventFactory.callPlayerInteractEvent(player, Action.PHYSICAL, pos, null, null, null);
            if (event.isCancelled()) {
                return;
            }
        } else {
            EntityInteractEvent event = new EntityInteractEvent(entity.bridge$getBukkitEntity(), CraftBlock.at(world, pos));
            Bukkit.getPluginManager().callEvent(event);
            if (event.isCancelled()) {
                return;
            }
        }
        arclight$withEntity(world.getBlockState(pos), world, pos, entity, original);
    }

    @WrapOperation(method = "useItemOn", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/RedStoneOreBlock;interact(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)V"))
    private void arclight$interact3(BlockState state, Level world, BlockPos pos, Operation<Void> original,
                                    ItemStack itemStack, BlockState hostState, Level hostWorld, BlockPos hostPos,
                                    Player player, InteractionHand hand, BlockHitResult hitResult) {
        arclight$withEntity(state, world, pos, player, original);
    }

    private static void arclight$withEntity(BlockState state, Level world, BlockPos pos, Entity entity, Operation<Void> original) {
        Entity previous = arclight$entity.get();
        arclight$entity.set(entity);
        try {
            original.call(state, world, pos);
        } finally {
            if (previous == null) {
                arclight$entity.remove();
            } else {
                arclight$entity.set(previous);
            }
        }
    }

    @Inject(method = "randomTick", cancellable = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"))
    private void arclight$blockFade(BlockState state, ServerLevel worldIn, BlockPos pos, RandomSource random, CallbackInfo ci) {
        if (CraftEventFactory.callBlockFadeEvent(worldIn, pos, state.setValue(RedStoneOreBlock.LIT, false)).isCancelled()) {
            ci.cancel();
        }
    }

    private static void interact(BlockState blockState, Level world, BlockPos blockPos, Entity entity) {
        Entity previous = arclight$entity.get();
        arclight$entity.set(entity);
        try {
            interact(blockState, world, blockPos);
        } finally {
            if (previous == null) {
                arclight$entity.remove();
            } else {
                arclight$entity.set(previous);
            }
        }
    }

    @WrapOperation(method = "interact(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"))
    private static boolean arclight$entityChangeBlock(Level world, BlockPos pos, BlockState state, int flags, Operation<Boolean> original) {
        return CraftEventFactory.callEntityChangeBlockEvent(arclight$entity.get(), pos, state)
            && original.call(world, pos, state, flags);
    }
}
