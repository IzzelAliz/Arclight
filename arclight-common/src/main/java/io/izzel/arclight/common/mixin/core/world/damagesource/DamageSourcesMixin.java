package io.izzel.arclight.common.mixin.core.world.damagesource;

import io.izzel.arclight.common.bridge.core.util.DamageSourceBridge;
import io.izzel.arclight.common.bridge.core.util.DamageSourcesBridge;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageSources;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;

@Mixin(DamageSources.class)
public abstract class DamageSourcesMixin implements DamageSourcesBridge {

    // @formatter:off
    @Shadow protected abstract DamageSource source(ResourceKey<DamageType> p_270957_);
    // @formatter:on

    @Shadow
    public abstract DamageSource badRespawnPointExplosion(Vec3 vec3);

    @Shadow
    public abstract DamageSource source(ResourceKey<DamageType> resourceKey, @org.jetbrains.annotations.Nullable Entity entity, @org.jetbrains.annotations.Nullable Entity entity2);

    public DamageSource melting;
    public DamageSource poison;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void arclight$init(RegistryAccess p_270740_, CallbackInfo ci) {
        this.melting = ((DamageSourceBridge) this.source(DamageTypes.ON_FIRE)).bridge$melting();
        this.poison = ((DamageSourceBridge) this.source(DamageTypes.MAGIC)).bridge$poison();
    }

    public DamageSource poison() {
        return poison;
    }

    public DamageSource melting() {
        return melting;
    }

    @Override
    public DamageSource bridge$poison() {
        return poison();
    }

    @Override
    public DamageSource bridge$melting() {
        return melting();
    }

    public DamageSource explosion(@Nullable Entity entity, @Nullable Entity entity1, ResourceKey<DamageType> resourceKey) {
        return this.source(resourceKey, entity, entity1);
    }

    public DamageSource badRespawnPointExplosion(Vec3 vec3d, org.bukkit.block.BlockState blockState) {
        return ((DamageSourceBridge) this.badRespawnPointExplosion(vec3d)).bridge$directBlockState(blockState);
    }
}
