package io.izzel.arclight.common.mixin.core.tileentity;

import com.google.common.collect.Lists;
import io.izzel.arclight.common.bridge.entity.EntityBridge;
import io.izzel.arclight.common.bridge.entity.MobEntityBridge;
import io.izzel.arclight.common.bridge.world.WorldBridge;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BeehiveBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v.block.CraftBlock;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityEnterBlockEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;
import java.util.List;

@Mixin(BeehiveBlockEntity.class)
public abstract class BeehiveTileEntityMixin extends TileEntityMixin {

    // @formatter:off
    @Shadow @Final private List<BeehiveBlockEntity.BeeData> stored;
    @Shadow protected abstract boolean releaseOccupant(BlockState p_235651_1_, BeehiveBlockEntity.BeeData p_235651_2_, @org.jetbrains.annotations.Nullable List<Entity> p_235651_3_, BeehiveBlockEntity.BeeReleaseStatus p_235651_4_);
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

    @Redirect(method = "emptyAllLivingFromHive", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/animal/Bee;setTarget(Lnet/minecraft/world/entity/LivingEntity;)V"))
    private void arclight$angryReason(Bee beeEntity, LivingEntity livingEntity) {
        ((MobEntityBridge) beeEntity).bridge$pushGoalTargetReason(EntityTargetEvent.TargetReason.CLOSEST_PLAYER, true);
        beeEntity.setTarget(livingEntity);
    }

    public List<Entity> tryReleaseBee(BlockState blockState, BeehiveBlockEntity.BeeReleaseStatus state, boolean force) {
        List<Entity> list = Lists.newArrayList();
        this.stored.removeIf(bee -> this.releaseBee(blockState, bee, list, state, force));
        return list;
    }

    @Inject(method = "addOccupantWithPresetTicks(Lnet/minecraft/world/entity/Entity;ZI)V", cancellable = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;stopRiding()V"))
    private void arclight$beeEnterBlock(Entity entity, boolean p_226962_2_, int p_226962_3_, CallbackInfo ci) {
        if (this.level != null) {
            EntityEnterBlockEvent event = new EntityEnterBlockEvent(((EntityBridge) entity).bridge$getBukkitEntity(), CraftBlock.at(this.level, this.getPos()));
            Bukkit.getPluginManager().callEvent(event);
            if (event.isCancelled()) {
                if (entity instanceof Bee) {
                    ((Bee) entity).setStayOutOfHiveCountdown(400);
                }
                ci.cancel();
            }
        }
    }

    private boolean releaseBee(BlockState blockState, BeehiveBlockEntity.BeeData bee, @Nullable List<Entity> list, BeehiveBlockEntity.BeeReleaseStatus state, boolean force) {
        arclight$force = force;
        try {
            return this.releaseOccupant(blockState, bee, list, state);
        } finally {
            arclight$force = false;
        }
    }

    private transient boolean arclight$force;

    @Redirect(method = "releaseOccupant", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;isNight()Z"))
    private boolean arclight$bypassNightCheck(Level world) {
        return !arclight$force && world.isNight();
    }

    @Redirect(method = "releaseOccupant", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getType()Lnet/minecraft/world/entity/EntityType;"))
    private EntityType<?> arclight$spawnFirst(Entity entity) {
        EntityType<?> type = entity.getType();
        if (type.is(EntityTypeTags.BEEHIVE_INHABITORS)) {
            ((WorldBridge) this.level).bridge$pushAddEntityReason(CreatureSpawnEvent.SpawnReason.BEEHIVE);
            if (!this.level.addFreshEntity(entity)) {
                return EntityType.ITEM_FRAME;
            } else {
                return type;
            }
        }
        return type;
    }

    @Redirect(method = "releaseOccupant", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"))
    private boolean arclight$addedBefore(Level world, Entity entityIn) {
        return true;
    }

    @Inject(method = "load", at = @At("RETURN"))
    private void arclight$readMax(BlockState state, CompoundTag compound, CallbackInfo ci) {
        if (compound.contains("Bukkit.MaxEntities")) {
            this.maxBees = compound.getInt("Bukkit.MaxEntities");
        }
    }

    @Inject(method = "save", at = @At("RETURN"))
    private void arclight$writeMax(CompoundTag compound, CallbackInfoReturnable<CompoundTag> cir) {
        compound.putInt("Bukkit.MaxEntities", this.maxBees);
    }
}
