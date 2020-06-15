package io.izzel.arclight.common.mixin.v1_15.tileentity;

import com.google.common.collect.Lists;
import io.izzel.arclight.common.bridge.entity.EntityBridge;
import io.izzel.arclight.common.bridge.world.WorldBridge;
import io.izzel.arclight.common.mixin.core.tileentity.TileEntityMixin;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.BeeEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.tileentity.BeehiveTileEntity;
import net.minecraft.world.World;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v.block.CraftBlock;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityEnterBlockEvent;
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

@Mixin(BeehiveTileEntity.class)
public abstract class BeehiveTileEntityMixin extends TileEntityMixin {

    // @formatter:off
    @Shadow @Final private List<BeehiveTileEntity.Bee> bees;
    @Shadow protected abstract boolean releaseBee(BlockState p_226967_1_, CompoundNBT p_226967_2_, @Nullable List<Entity> p_226967_3_, BeehiveTileEntity.State p_226967_4_);
    // @formatter:on

    public int maxBees = 3;

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public boolean isFullOfBees() {
        return this.bees.size() >= maxBees;
    }

    public List<Entity> tryReleaseBee(BlockState blockState, BeehiveTileEntity.State state, boolean force) {
        List<Entity> list = Lists.newArrayList();
        this.bees.removeIf(bee -> this.releaseBee(blockState, bee.entityData, list, state, force));
        return list;
    }

    @Inject(method = "tryEnterHive(Lnet/minecraft/entity/Entity;ZI)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;stopRiding()V"))
    private void arclight$beeEnterBlock(Entity entity, boolean p_226962_2_, int p_226962_3_, CallbackInfo ci) {
        if (this.world != null) {
            EntityEnterBlockEvent event = new EntityEnterBlockEvent(((EntityBridge) entity).bridge$getBukkitEntity(), CraftBlock.at(this.world, this.getPos()));
            Bukkit.getPluginManager().callEvent(event);
            if (event.isCancelled()) {
                if (entity instanceof BeeEntity) {
                    ((BeeEntity) entity).setStayOutOfHiveCountdown(400);
                }
                ci.cancel();
            }
        }
    }

    private boolean releaseBee(BlockState blockState, CompoundNBT nbt, @Nullable List<Entity> list, BeehiveTileEntity.State state, boolean force) {
        arclight$force = force;
        try {
            return this.releaseBee(blockState, nbt, list, state);
        } finally {
            arclight$force = false;
        }
    }

    private transient boolean arclight$force;

    @Redirect(method = "releaseBee", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;isNightTime()Z"))
    private boolean arclight$bypassNightCheck(World world) {
        return !arclight$force && world.isNightTime();
    }

    @Redirect(method = "releaseBee", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;getType()Lnet/minecraft/entity/EntityType;"))
    private EntityType<?> arclight$spawnFirst(Entity entity) {
        EntityType<?> type = entity.getType();
        if (type.isContained(EntityTypeTags.BEEHIVE_INHABITORS)) {
            ((WorldBridge) this.world).bridge$pushAddEntityReason(CreatureSpawnEvent.SpawnReason.BEEHIVE);
            if (!this.world.addEntity(entity)) {
                return EntityType.ITEM_FRAME;
            } else {
                return type;
            }
        }
        return type;
    }

    @Redirect(method = "releaseBee", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;addEntity(Lnet/minecraft/entity/Entity;)Z"))
    private boolean arclight$addedBefore(World world, Entity entityIn) {
        return true;
    }

    @Inject(method = "read", at = @At("RETURN"))
    private void arclight$readMax(CompoundNBT compound, CallbackInfo ci) {
        if (compound.contains("Bukkit.MaxEntities")) {
            this.maxBees = compound.getInt("Bukkit.MaxEntities");
        }
    }

    @Inject(method = "write", at = @At("RETURN"))
    private void arclight$writeMax(CompoundNBT compound, CallbackInfoReturnable<CompoundNBT> cir) {
        compound.putInt("Bukkit.MaxEntities", this.maxBees);
    }
}
