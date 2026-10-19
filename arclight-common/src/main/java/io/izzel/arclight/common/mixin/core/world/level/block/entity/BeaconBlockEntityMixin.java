package io.izzel.arclight.common.mixin.core.world.level.block.entity;

import io.izzel.arclight.common.bridge.core.world.level.block.entity.BeaconBlockEntityBridge;
import io.izzel.arclight.common.mod.mixins.annotation.TransformAccess;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;
import org.bukkit.craftbukkit.potion.CraftPotionUtil;
import org.bukkit.potion.PotionEffect;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BeaconBlockEntity.class)
public abstract class BeaconBlockEntityMixin implements BeaconBlockEntityBridge {

    @com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation(method = "applyEffects", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;addEffect(Lnet/minecraft/world/effect/MobEffectInstance;)Z"))
    private static boolean arclight$beaconCause(Player player, MobEffectInstance effect,
            com.llamalad7.mixinextras.injector.wrapoperation.Operation<Boolean> original) {
        var bridge = (io.izzel.arclight.common.bridge.core.world.entity.LivingEntityBridge) player;
        var previous = bridge.bridge$getEffectCause().orElse(null);
        bridge.bridge$pushEffectCause(org.bukkit.event.entity.EntityPotionEffectEvent.Cause.BEACON);
        try {
            return original.call(player, effect);
        } finally {
            bridge.bridge$pushEffectCause(previous);
        }
    }

    @com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation(method = "loadEffect", at = @At(value = "INVOKE", target = "Ljava/util/Optional;filter(Ljava/util/function/Predicate;)Ljava/util/Optional;"))
    private static java.util.Optional<Holder<MobEffect>> arclight$persistCustomEffect(java.util.Optional<Holder<MobEffect>> effect,
            java.util.function.Predicate<? super Holder<MobEffect>> predicate,
            com.llamalad7.mixinextras.injector.wrapoperation.Operation<java.util.Optional<Holder<MobEffect>>> original) {
        return effect;
    }

    // @formatter:off
    @Shadow public int levels;
    @Shadow @Nullable public Holder<MobEffect> primaryPower;
    @Shadow @Nullable public Holder<MobEffect> secondaryPower;
    // @formatter:on

    // CraftBeacon.getEntitiesInRange invokes this exact static descriptor on BeaconBlockEntity.
    // Match the fixed CraftBukkit beacon scan, including the upward world-height extension.
    // Merge as private; Arclight's postApply publishes the CraftBukkit public static ABI.
    @TransformAccess(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC)
    private static List<Player> getHumansInRange(Level level, BlockPos worldPosition, int levels) {
        double range = (double) (levels * 10 + 10);
        AABB bounds = new AABB(worldPosition).inflate(range).expandTowards(0.0D, (double) level.getHeight(), 0.0D);
        return level.getEntitiesOfClass(Player.class, bounds);
    }

    @Inject(method = "loadAdditional", at = @At("RETURN"))
    public void arclight$level(ValueInput input, CallbackInfo ci) {
        this.levels = input.getIntOr("Levels", this.levels);
    }

    public PotionEffect getPrimaryEffect() {
        return (this.primaryPower != null) ? CraftPotionUtil.toBukkit(new MobEffectInstance(this.primaryPower, this.getLevel(), this.getAmplification(), true, true)) : null;
    }

    public PotionEffect getSecondaryEffect() {
        return (this.hasSecondaryEffect()) ? CraftPotionUtil.toBukkit(new MobEffectInstance(this.secondaryPower, getLevel(), getAmplification(), true, true)) : null;
    }

    private byte getAmplification() {
        byte b0 = 0;
        if (this.levels >= 4 && java.util.Objects.equals(this.primaryPower, this.secondaryPower)) {
            b0 = 1;
        }
        return b0;
    }

    private int getLevel() {
        int i = (9 + this.levels * 2) * 20;
        return i;
    }

    private boolean hasSecondaryEffect() {
        if (this.levels >= 4 && !java.util.Objects.equals(this.primaryPower, this.secondaryPower) && this.secondaryPower != null) {
            return true;
        }
        return false;
    }

    @Override
    public PotionEffect bridge$getPrimaryEffect() {
        return getPrimaryEffect();
    }

    @Override
    public PotionEffect bridge$getSecondaryEffect() {
        return getSecondaryEffect();
    }
}
