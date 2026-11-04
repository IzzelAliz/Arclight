package io.izzel.arclight.common.mixin.core.world.damagesource;

import io.izzel.arclight.common.bridge.core.world.damagesource.DamageSourceBridge;
import net.minecraft.core.Holder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import javax.annotation.Nullable;

@Mixin(DamageSource.class)
public abstract class DamageSourceMixin implements DamageSourceBridge {

    // @formatter:off
    @Shadow @Nullable public Entity getEntity() { return null; }
    @Shadow @Final @org.jetbrains.annotations.Nullable private Entity causingEntity;
    @Shadow @Final private Holder<DamageType> type;
    @Shadow @Final @org.jetbrains.annotations.Nullable private Entity directEntity;
    @Shadow @Final @org.jetbrains.annotations.Nullable private Vec3 damageSourcePosition;
    // @formatter:on

    private org.bukkit.block.Block eventBlockDamager; // Paper added
    private org.bukkit.block.BlockState fromBlockSnapshot; // Paper added
    private boolean withSweep;
    private boolean melting;
    private boolean poison;
    private Entity eventEntityDamager = null; // Paper added
    private Entity customCausingEntityDamager = null;

    @Unique @Nullable private DamageCause knownCause; // Paper added
    @Unique private boolean critical; // Paper added

    @Override
    @Nullable
    public DamageCause bridge$knownCause() {
        return this.knownCause;
    }

    @Override
    public DamageSource bridge$knownCause(@Nullable DamageCause cause) {
        return ((DamageSourceBridge) this.cloneInstance()).bridge$setKnownCause(cause);
    }

    @Override
    public DamageSource bridge$setKnownCause(@Nullable DamageCause cause) {
        this.knownCause = cause;
        return (DamageSource) (Object) this;
    }

    @Override
    public boolean bridge$isCritical() {
        return this.critical;
    }

    @Override
    public DamageSource bridge$critical() {
        return ((DamageSourceBridge) this.cloneInstance()).bridge$setCritical(true);
    }

    @Override
    public DamageSource bridge$setCritical(boolean critical) {
        this.critical = critical;
        return (DamageSource) (Object) this;
    }

    public boolean isSweep() {
        return withSweep;
    }

    @Override
    public boolean bridge$isSweep() {
        return isSweep();
    }

    public DamageSource sweep() {
        withSweep = true;
        return (DamageSource) (Object) this;
    }

    @Override
    public DamageSource bridge$sweep() {
        return sweep();
    }

    public boolean isMelting() {
        return melting;
    }

    public DamageSource melting() {
        this.melting = true;
        return (DamageSource) (Object) this;
    }

    @Override
    public DamageSource bridge$melting() {
        return melting();
    }

    public boolean isPoison() {
        return poison;
    }

    public DamageSource poison() {
        this.poison = true;
        return (DamageSource) (Object) this;
    }

    @Override
    public DamageSource bridge$poison() {
        return poison();
    }

    public Entity getDamager() {
        // Arclight: Blame: used causingEntity instead of directEntity and bump into problems :(
        return this.eventEntityDamager == null ? this.directEntity : this.eventEntityDamager;
    }

    @Override
    public Entity bridge$getCausingEntity() {
        return this.getDamager();
    }

    public DamageSource customEntityDamager(Entity entity) {
        // This method is not intended for change the causing entity if is already set
        // also is only necessary if the entity passed is not the direct entity or different from the current causingEntity
        if (this.eventEntityDamager != null || this.directEntity == entity || this.causingEntity == entity) {
            return (DamageSource) (Object) this;
        }
        var src = cloneInstance();
        return ((DamageSourceBridge) src).bridge$setCustomCausingEntity(entity);
    }

    public Entity getCausingDamager() {
        return (this.customCausingEntityDamager != null) ? this.customCausingEntityDamager : this.causingEntity;
    }

    @Override
    public Entity bridge$getCausingEntityDamager() {
        return this.getCausingDamager();
    }

    @Override
    public DamageSource bridge$customCausingEntity(Entity entity) {
        return customEntityDamager(entity);
    }

    @Override
    public DamageSource bridge$setCustomCausingEntity(Entity entity) {
        this.eventEntityDamager = entity;
        return (DamageSource) (Object) this;
    }

    public DamageSource customCausingEntityDamager(Entity entity) {
        // This method is not intended for change the causing entity if is already set
        // also is only necessary if the entity passed is not the direct entity or different from the current causingEntity
        if (this.customCausingEntityDamager != null || this.directEntity == entity || this.causingEntity == entity) {
            return (DamageSource) (Object) this;
        }
        var src = cloneInstance();
        return ((DamageSourceBridge) src).bridge$setCustomCausingEntityDamager(entity);
    }

    @Override
    public DamageSource bridge$customCausingEntityDamager(Entity entity) {
        return customCausingEntityDamager(entity);
    }

    @Override
    public DamageSource bridge$setCustomCausingEntityDamager(Entity entity) {
        this.customCausingEntityDamager = entity;
        return (DamageSource) (Object) this;
    }

    public Block getDirectBlock() {
        return this.eventBlockDamager;
    }

    @Override
    public Block bridge$directBlock() {
        return this.getDirectBlock();
    }

    @Override
    public DamageSource bridge$directBlock(Block block) {
        return ((DamageSourceBridge) cloneInstance()).bridge$setDirectBlock(block);
    }

    @Override
    public DamageSource bridge$setDirectBlock(Block block) {
        this.eventBlockDamager = block;
        return (DamageSource) (Object) this;
    }

    public org.bukkit.block.BlockState getDirectBlockState() {
        return this.fromBlockSnapshot;
    }

    public DamageSource directBlockState(org.bukkit.block.BlockState blockState) {
        if (blockState == null) {
            return (DamageSource) (Object) this;
        }
        // Cloning the instance lets us return unique instances of DamageSource without affecting constants defined in DamageSources
        DamageSource damageSource = this.cloneInstance();
        ((DamageSourceBridge) damageSource).bridge$setDirectBlockState(blockState);
        return damageSource;
    }

    @Override
    public BlockState bridge$directBlockState() {
        return this.fromBlockSnapshot;
    }

    @Override
    public DamageSource bridge$directBlockState(BlockState block) {
        return directBlockState(block);
    }

    @Override
    public DamageSource bridge$setDirectBlockState(BlockState block) {
        this.fromBlockSnapshot = block;
        return (DamageSource) (Object) this;
    }

    private DamageSource cloneInstance() {
        var damageSource = new DamageSource(this.type, this.directEntity, this.causingEntity, this.damageSourcePosition);
        var br = (DamageSourceBridge) damageSource;
        br.bridge$setDirectBlock(this.bridge$directBlock());
        br.bridge$setDirectBlockState(this.bridge$directBlockState());
        br.bridge$setCustomCausingEntity(this.eventEntityDamager);
        br.bridge$setCustomCausingEntityDamager(this.customCausingEntityDamager);
        br.bridge$setKnownCause(this.knownCause);
        br.bridge$setCritical(this.critical);
        if (this.withSweep) {
            br.bridge$sweep();
        }
        if (this.poison) {
            br.bridge$poison();
        }
        if (this.melting) {
            br.bridge$melting();
        }
        return damageSource;
    }

    public org.bukkit.block.Block eventBlockDamager() {
        return this.bridge$directBlock();
    }

    public org.bukkit.block.BlockState causingBlockSnapshot() {
        return this.bridge$directBlockState();
    }

    public boolean isCritical() {
        return this.bridge$isCritical();
    }

}
