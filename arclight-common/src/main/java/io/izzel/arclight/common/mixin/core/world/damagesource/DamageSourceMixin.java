package io.izzel.arclight.common.mixin.core.world.damagesource;

import io.izzel.arclight.common.bridge.core.util.DamageSourceBridge;
import net.minecraft.core.Holder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.bukkit.block.Block;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

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

    private org.bukkit.block.Block directBlock;
    private boolean withSweep;
    private boolean melting;
    private boolean poison;
    private Entity customCausingEntity = null;

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

    public Entity getCausingEntity() {
        return this.customCausingEntity == null ? this.causingEntity : this.customCausingEntity;
    }

    @Override
    public Entity bridge$getCausingEntity() {
        return this.getCausingEntity();
    }

    @Override
    public DamageSource bridge$customCausingEntity(Entity entity) {
        // This method is not intended for change the causing entity if is already set
        // also is only necessary if the entity passed is not the direct entity or different from the current causingEntity
        if (this.customCausingEntity != null || this.directEntity == entity || this.causingEntity == entity) {
            return (DamageSource) (Object) this;
        }
        var src = cloneInstance();
        return ((DamageSourceBridge) src).bridge$setCustomCausingEntity(entity);
    }

    @Override
    public DamageSource bridge$setCustomCausingEntity(Entity entity) {
        this.customCausingEntity = entity;
        return (DamageSource) (Object) this;
    }

    public Block getDirectBlock() {
        return this.directBlock;
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
        this.directBlock = block;
        return (DamageSource) (Object) this;
    }

    private DamageSource cloneInstance() {
        var damageSource = new DamageSource(this.type, this.directEntity, this.causingEntity, this.damageSourcePosition);
        var br = (DamageSourceBridge) damageSource;
        br.bridge$setDirectBlock(this.bridge$directBlock());
        br.bridge$setCustomCausingEntity(this.customCausingEntity);
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
}
