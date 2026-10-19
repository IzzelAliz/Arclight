package io.izzel.arclight.common.mixin.core.world.level.block.entity;

import com.google.common.collect.Lists;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import io.izzel.arclight.common.bridge.core.world.level.WorldBridge;
import net.minecraft.util.RandomSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import java.util.function.Predicate;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.BeehiveBlock;
import net.minecraft.world.level.gameevent.GameEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.spongepowered.asm.mixin.Unique;
import java.util.ArrayList;
import io.izzel.arclight.common.bridge.core.world.entity.MobBridge;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.bee.Bee;
import net.minecraft.world.attribute.EnvironmentAttribute;
import net.minecraft.world.attribute.EnvironmentAttributeSystem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BeehiveBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.block.CraftBlock;
import org.bukkit.event.entity.EntityEnterBlockEvent;
import org.bukkit.event.entity.EntityRemoveEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;
import java.util.List;

@Mixin(BeehiveBlockEntity.class)
public abstract class BeehiveBlockEntityMixin extends BlockEntityMixin {

    // @formatter:off
    @Shadow private List<BeehiveBlockEntity.BeeData> stored;
    @Shadow @Nullable public BlockPos savedFlowerPos;
    @Shadow private static boolean releaseOccupant(Level arg, BlockPos arg2, BlockState arg3, BeehiveBlockEntity.Occupant arg4, @org.jetbrains.annotations.Nullable List<Entity> list, BeehiveBlockEntity.BeeReleaseStatus arg5, @org.jetbrains.annotations.Nullable BlockPos arg6) { return false; }
    // @formatter:on

    public int maxBees = 3;

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public boolean isFull() {
        return this.stored.size() >= maxBees;
    }

    @Redirect(method = "emptyAllLivingFromHive", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/animal/bee/Bee;setTarget(Lnet/minecraft/world/entity/LivingEntity;)V"))
    private void arclight$angryReason(Bee beeEntity, LivingEntity livingEntity) {
        ((MobBridge) beeEntity).bridge$pushGoalTargetReason(EntityTargetEvent.TargetReason.CLOSEST_PLAYER, true);
        beeEntity.setTarget(livingEntity);
    }

    public List<Entity> releaseBees(BlockState blockState, BeehiveBlockEntity.BeeReleaseStatus state, boolean force) {
        List<Entity> list = Lists.newArrayList();
        this.stored.removeIf(bee -> releaseBee(level, worldPosition, blockState, bee, list, state, this.savedFlowerPos, force));
        if (!list.isEmpty() && this.level != null) {
            setChanged(this.level, this.worldPosition, this.getBlockState());
        }
        return list;
    }

    @Redirect(method = "addOccupant", at = @At(value = "INVOKE", remap = false, target = "Ljava/util/List;size()I"))
    private int arclight$maxBee(List<?> list) {
        return list.size() < this.maxBees ? 1 : 3;
    }

    @Inject(method = "addOccupant", cancellable = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/animal/bee/Bee;stopRiding()V"))
    private void arclight$beeEnterBlock(Bee entity, CallbackInfo ci) {
        if (this.level != null) {
            EntityEnterBlockEvent event = new EntityEnterBlockEvent(entity.bridge$getBukkitEntity(), CraftBlock.at(this.level, this.worldPosition));
            Bukkit.getPluginManager().callEvent(event);
            if (event.isCancelled()) {
                entity.setStayOutOfHiveCountdown(400);
                ci.cancel();
            }
        }
    }

    @Inject(method = "addOccupant", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/animal/bee/Bee;discard()V"))
    private void arclight$enterBlockCause(Bee entity, CallbackInfo ci) {
        entity.bridge().bridge$pushEntityRemoveCause(EntityRemoveEvent.Cause.ENTER_BLOCK);
    }

    private static boolean releaseBee(Level world, BlockPos pos, BlockState state, BeehiveBlockEntity.BeeData beeData, @Nullable List<Entity> list, BeehiveBlockEntity.BeeReleaseStatus status, @Nullable BlockPos pos1, boolean force) {
        boolean previousForce = arclight$force;
        arclight$force = force;
        try {
            return releaseOccupant(world, pos, state, beeData.toOccupant(), list, status, pos1);
        } finally {
            arclight$force = previousForce;
        }
    }

    private static transient boolean arclight$force;

    @Redirect(method = "releaseOccupant", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/attribute/EnvironmentAttributeSystem;getValue(Lnet/minecraft/world/attribute/EnvironmentAttribute;Lnet/minecraft/core/BlockPos;)Ljava/lang/Object;"))
    private static Object arclight$bypassNightCheck(EnvironmentAttributeSystem system, EnvironmentAttribute<Boolean> attribute, BlockPos pos) {
        return !arclight$force && system.getValue(attribute, pos);
    }

    // Eligible native release only: normal false delays the next retry; throws propagate untouched.
    @com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation(method = "tickOccupants", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/entity/BeehiveBlockEntity;releaseOccupant(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/entity/BeehiveBlockEntity$Occupant;Ljava/util/List;Lnet/minecraft/world/level/block/entity/BeehiveBlockEntity$BeeReleaseStatus;Lnet/minecraft/core/BlockPos;)Z"))
    private static boolean arclight$retryRelease(Level world, BlockPos pos, BlockState state, BeehiveBlockEntity.Occupant occupant, @Nullable List<Entity> entities, BeehiveBlockEntity.BeeReleaseStatus status, @Nullable BlockPos flower, com.llamalad7.mixinextras.injector.wrapoperation.Operation<Boolean> original, @com.llamalad7.mixinextras.sugar.Local BeehiveBlockEntity.BeeData beeData) {
        boolean released = original.call(world, pos, state, occupant, entities, status, flower);
        if (!released) {
            beeData.ticksInHive = beeData.toOccupant().minTicksInHive() / 2;
        }
        return released;
    }

    // The bee-only commit block is skipped here; the late add callsite remains the
    // composable platform operation and becomes the acceptance gate below.
    @Decorate(method = "releaseOccupant", inject = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;getRandom()Lnet/minecraft/util/RandomSource;"))
    private static void arclight$deferBeeCommit() throws Throwable {
        throw DecorationOps.jumpToCodeBlockEnd();
    }

    @WrapOperation(method = "releaseOccupant", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;playSound(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/core/BlockPos;Lnet/minecraft/sounds/SoundEvent;Lnet/minecraft/sounds/SoundSource;FF)V"))
    private static void arclight$deferExitSound(Level world, Entity source, BlockPos pos, SoundEvent sound, SoundSource category, float volume, float pitch, Operation<Void> original, @Share("releaseEffects") LocalRef<List<Runnable>> effects) {
        List<Runnable> pending = new ArrayList<>();
        pending.add(() -> original.call(world, source, pos, sound, category, volume, pitch));
        effects.set(pending);
    }

    @WrapOperation(method = "releaseOccupant", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;gameEvent(Lnet/minecraft/core/Holder;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/gameevent/GameEvent$Context;)V"))
    private static void arclight$deferExitEvent(Level world, Holder<GameEvent> event, BlockPos pos, GameEvent.Context context, Operation<Void> original, @Share("releaseEffects") LocalRef<List<Runnable>> effects) {
        effects.get().add(() -> original.call(world, event, pos, GameEvent.Context.of(context.sourceEntity(), world.getBlockState(pos))));
    }

    @WrapOperation(method = "releaseOccupant", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"))
    private static boolean arclight$releaseBeforeCommit(Level world, Entity entity, Operation<Boolean> original, Level enclosingLevel, BlockPos pos, BlockState state, BeehiveBlockEntity.Occupant occupant, @Nullable List<Entity> list, BeehiveBlockEntity.BeeReleaseStatus status, @Nullable BlockPos flower, @Local Direction direction, @Local boolean blocked, @Share("releaseEffects") LocalRef<List<Runnable>> effects) {
        if (entity instanceof Bee) {
            double offset = blocked ? 0.0D : 0.55D + entity.getBbWidth() / 2.0F;
            entity.snapTo(pos.getX() + 0.5D + offset * direction.getStepX(), pos.getY() + 0.5D - entity.getBbHeight() / 2.0F, pos.getZ() + 0.5D + offset * direction.getStepZ(), entity.getYRot(), entity.getXRot());
        }
        ((WorldBridge) world).bridge$pushAddEntityReason(CreatureSpawnEvent.SpawnReason.BEEHIVE);
        if (!original.call(world, entity)) {
            return false;
        }
        if (entity instanceof Bee bee) {
            RandomSource random = world.getRandom();
            if (flower != null && !bee.hasSavedFlowerPos() && random.nextFloat() < 0.9F) {
                bee.setSavedFlowerPos(flower);
            }
            if (status == BeehiveBlockEntity.BeeReleaseStatus.HONEY_DELIVERED) {
                bee.dropOffNectar();
                if (state.is(BlockTags.BEEHIVES, block -> block.hasProperty(BeehiveBlock.HONEY_LEVEL))) {
                    int honey = BeehiveBlockEntity.getHoneyLevel(state);
                    if (honey < 5) {
                        int increment = random.nextInt(100) == 0 ? 2 : 1;
                        if (honey + increment > 5) {
                            --increment;
                        }
                        world.setBlockAndUpdate(pos, state.setValue(BeehiveBlock.HONEY_LEVEL, honey + increment));
                    }
                }
            }
            if (list != null) {
                list.add(bee);
            }
        }
        for (Runnable effect : effects.get()) {
            effect.run();
        }
        return true;
    }

    // Fresh identity at the native clear point preserves active aliases during reentrant loading.
    @Redirect(method = {"loadAdditional", "applyImplicitComponents"}, at = @At(value = "INVOKE", target = "Ljava/util/List;clear()V", remap = false))
    private void arclight$freshStored(List<?> previousStored) {
        this.stored = Lists.newArrayList();
    }

    @Inject(method = "loadAdditional", at = @At("RETURN"))
    private void arclight$readMax(ValueInput input, CallbackInfo ci) {
        this.maxBees = input.getIntOr("Bukkit.MaxEntities", 3);
    }

    @Inject(method = "saveAdditional", at = @At("RETURN"))
    private void arclight$writeMax(ValueOutput output, CallbackInfo ci) {
        output.putInt("Bukkit.MaxEntities", this.maxBees);
    }
}
