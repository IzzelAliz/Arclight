package io.izzel.arclight.common.mixin.core.world.level;

import io.izzel.arclight.common.bridge.core.world.damagesource.DamageSourceBridge;
import io.izzel.arclight.common.bridge.core.world.level.ExplosionBridge;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import io.izzel.arclight.mixin.Local;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.ServerExplosion;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.TntBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.TNTPrimeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.function.BiConsumer;

@Mixin(ServerExplosion.class)
public abstract class ExplosionMixin implements ExplosionBridge {

    // @formatter:off
    @Shadow @Final private ServerLevel level;
    @Shadow @Final private Explosion.BlockInteraction blockInteraction;
    @Shadow @Mutable @Final private float radius;
    @Shadow @Final private Vec3 center;
    @Shadow @Final private Entity source;
    @Accessor("source") public abstract Entity bridge$getExploder();
    @Accessor("radius") public abstract float bridge$getSize();
    @Accessor("radius") public abstract void bridge$setSize(float size);
    @Accessor("blockInteraction") public abstract Explosion.BlockInteraction bridge$getMode();
    @Shadow @Final @Mutable private DamageSource damageSource;
    @Shadow public abstract Explosion.BlockInteraction getBlockInteraction();
    // @formatter:on

    public float yield;
    public boolean wasCanceled = false;

    @Override
    public float bridge$getYield() {
        return this.yield;
    }

    @Inject(method = "<init>(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/damagesource/DamageSource;Lnet/minecraft/world/level/ExplosionDamageCalculator;Lnet/minecraft/world/phys/Vec3;FZLnet/minecraft/world/level/Explosion$BlockInteraction;)V", at = @At("RETURN"))
    private void arclight$adjustSize(ServerLevel level, Entity entity, DamageSource damageSource,
                                    ExplosionDamageCalculator explosionDamageCalculator, Vec3 center,
                                    float radius, boolean fire, Explosion.BlockInteraction blockInteraction,
                                    CallbackInfo ci) {
        this.radius = Math.max(radius, 0.0F);
        this.yield = this.blockInteraction == Explosion.BlockInteraction.DESTROY_WITH_DECAY ? 1.0F / this.radius : 1.0F;
        this.damageSource = ((DamageSourceBridge) this.damageSource).bridge$customCausingEntity(entity);
    }

    @Inject(method = "explode", cancellable = true, at = @At("HEAD"))
    private void arclight$returnRadius(CallbackInfoReturnable<Integer> cir) {
        if (this.radius < 0.1F) {
            cir.setReturnValue(0);
        }
    }

    @Decorate(method = "explode", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/ServerExplosion;interactsWithBlocks()Z"))
    private boolean arclight$keepExplodeEvent(ServerExplosion explosion, @Local(ordinal = -1) List<BlockPos> targetBlocks) throws Throwable {
        boolean interactsWithBlocks = (boolean) DecorationOps.callsite().invoke(explosion);
        if (!interactsWithBlocks) {
            net.minecraft.util.Util.shuffle(targetBlocks, this.level.random);
            this.wasCanceled = this.arclight$callExplodeEvent(targetBlocks);
        }
        return interactsWithBlocks;
    }

    @Decorate(method = "interactWithBlocks", inject = true, at = @At(value = "INVOKE", shift = At.Shift.AFTER, target = "Lnet/minecraft/util/Util;shuffle(Ljava/util/List;Lnet/minecraft/util/RandomSource;)V"))
    private void arclight$blockExplode(List<BlockPos> targetBlocks) throws Throwable {
        if (this.arclight$callExplodeEvent(targetBlocks)) {
            this.wasCanceled = true;
            DecorationOps.cancel().invoke();
            return;
        }
        DecorationOps.blackhole().invoke();
    }

    @Decorate(method = "interactWithBlocks", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;onExplosionHit(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/Explosion;Ljava/util/function/BiConsumer;)V"))
    private void arclight$tntPrime(BlockState state, ServerLevel level, BlockPos pos, Explosion explosion, BiConsumer<ItemStack, BlockPos> drops) throws Throwable {
        if (state.getBlock() instanceof TntBlock) {
            var sourceBlock = this.source == null ? BlockPos.containing(this.center) : null;
            if (!CraftEventFactory.callTNTPrimeEvent(this.level, pos, TNTPrimeEvent.PrimeCause.EXPLOSION, this.source, sourceBlock)) {
                this.level.sendBlockUpdated(pos, Blocks.AIR.defaultBlockState(), state, 3);
                return;
            }
        }
        DecorationOps.callsite().invoke(state, level, pos, explosion, drops);
    }

    @Inject(method = "createFire", at = @At("HEAD"), cancellable = true)
    private void arclight$cancelFire(List<BlockPos> blocks, CallbackInfo ci) {
        if (this.wasCanceled) ci.cancel();
    }

    @Decorate(method = "createFire", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;setBlockAndUpdate(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Z"))
    private boolean arclight$blockIgnite(ServerLevel level, BlockPos blockPos, BlockState blockState) throws Throwable {
        BlockIgniteEvent event = CraftEventFactory.callBlockIgniteEvent(this.level, blockPos, (Explosion) (Object) this);
        return event.isCancelled() ? false : (boolean) DecorationOps.callsite().invoke(level, blockPos, blockState);
    }

    @Inject(method = "addOrAppendStack", cancellable = true, at = @At("HEAD"))
    private static void arclight$fix(List<?> stacks, ItemStack stack, BlockPos pos, CallbackInfo ci) {
        if (stack.isEmpty()) {
            ci.cancel();
        }
    }

    @Override
    public boolean bridge$wasCancelled() {
        return this.wasCanceled;
    }

    @Unique
    private boolean arclight$callExplodeEvent(List<BlockPos> targetBlocks) {
        org.bukkit.World world = this.level.bridge$getWorld();
        Location location = new Location(world, this.center.x, this.center.y, this.center.z);
        List<org.bukkit.block.Block> blockList = new ObjectArrayList<>();
        for (int i = targetBlocks.size() - 1; i >= 0; i--) {
            BlockPos blockPos = targetBlocks.get(i);
            org.bukkit.block.Block block = world.getBlockAt(blockPos.getX(), blockPos.getY(), blockPos.getZ());
            if (!block.getType().isAir()) {
                blockList.add(block);
            }
        }

        boolean cancelled;
        List<org.bukkit.block.Block> bukkitBlocks;
        if (this.source != null) {
            EntityExplodeEvent event = CraftEventFactory.callEntityExplodeEvent(this.source, blockList, this.yield, this.getBlockInteraction());
            cancelled = event.isCancelled();
            bukkitBlocks = event.blockList();
            this.yield = event.getYield();
        } else {
            org.bukkit.block.Block block = location.getBlock();
            org.bukkit.block.BlockState blockState = ((DamageSourceBridge) this.damageSource).bridge$directBlockState() != null
                ? ((DamageSourceBridge) this.damageSource).bridge$directBlockState() : block.getState();
            BlockExplodeEvent event = CraftEventFactory.callBlockExplodeEvent(block, blockState, blockList, this.yield, this.getBlockInteraction());
            cancelled = event.isCancelled();
            bukkitBlocks = event.blockList();
            this.yield = event.getYield();
        }

        targetBlocks.clear();
        for (org.bukkit.block.Block block : bukkitBlocks) {
            targetBlocks.add(new BlockPos(block.getX(), block.getY(), block.getZ()));
        }
        return cancelled;
    }
}
