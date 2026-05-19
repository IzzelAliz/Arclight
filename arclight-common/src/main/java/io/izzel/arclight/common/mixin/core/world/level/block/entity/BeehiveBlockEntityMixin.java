package io.izzel.arclight.common.mixin.core.world.level.block.entity;

import com.google.common.collect.Lists;
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
import org.bukkit.craftbukkit.v.block.CraftBlock;
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
        arclight$force = force;
        try {
            return releaseOccupant(world, pos, state, beeData.toOccupant(), list, status, pos1);
        } finally {
            arclight$force = false;
        }
    }

    private static transient boolean arclight$force;

    @Redirect(method = "releaseOccupant", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/attribute/EnvironmentAttributeSystem;getValue(Lnet/minecraft/world/attribute/EnvironmentAttribute;Lnet/minecraft/core/BlockPos;)Ljava/lang/Object;"))
    private static Object arclight$bypassNightCheck(EnvironmentAttributeSystem system, EnvironmentAttribute<Boolean> attribute, BlockPos pos) {
        return !arclight$force && system.getValue(attribute, pos);
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
