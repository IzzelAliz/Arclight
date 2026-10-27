package io.izzel.arclight.common.mixin.core.world.level.block.entity;

import com.destroystokyo.paper.event.block.BeaconEffectEvent;
import io.izzel.arclight.common.bridge.core.world.entity.LivingEntityBridge;
import io.izzel.arclight.common.bridge.core.world.level.block.entity.BeaconBlockEntityBridge;
import io.izzel.arclight.common.mod.mixins.annotation.TransformAccess;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import io.izzel.arclight.mixin.Local;
import io.papermc.paper.event.block.BeaconActivatedEvent;
import io.papermc.paper.event.block.BeaconDeactivatedEvent;
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
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.craftbukkit.block.CraftBlock;
import org.bukkit.craftbukkit.potion.CraftPotionUtil;
import org.bukkit.event.entity.EntityPotionEffectEvent;
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

    // The fourth read is the final scan-completion check, after the periodic effect pass.
    // Paper reports level transitions here even when the beam scan has not completed.
    @Decorate(method = "tick(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/entity/BeaconBlockEntity;)V",
        inject = true, at = @At(value = "FIELD", target = "Lnet/minecraft/world/level/block/entity/BeaconBlockEntity;lastCheckY:I",
            opcode = Opcodes.GETFIELD, ordinal = 3), require = 1, allow = 1)
    private static void arclight$beaconActivation(Level level, BlockPos pos, BlockState state, BeaconBlockEntity entity,
                                                 @Local(ordinal = 4) int previousLevels) {
        if (previousLevels <= 0 && entity.levels > 0) {
            new BeaconActivatedEvent(CraftBlock.at(level, pos)).callEvent();
        } else if (previousLevels > 0 && entity.levels <= 0) {
            new BeaconDeactivatedEvent(CraftBlock.at(level, pos)).callEvent();
        }
    }

    @Decorate(method = "setRemoved()V", inject = true, at = @At("HEAD"), require = 1, allow = 1)
    private void arclight$beaconRemoval() {
        BeaconBlockEntity beacon = (BeaconBlockEntity) (Object) this;
        // Paper dispatches on every removal, including an already inactive beacon.
        new BeaconDeactivatedEvent(CraftBlock.at(beacon.getLevel(), beacon.getBlockPos())).callEvent();
    }

    @Decorate(method = "applyEffects(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;ILnet/minecraft/core/Holder;Lnet/minecraft/core/Holder;)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;addEffect(Lnet/minecraft/world/effect/MobEffectInstance;)Z",
            ordinal = 0), require = 1, allow = 1)
    private static boolean arclight$primaryBeaconEffect(Player player, MobEffectInstance effect,
                                                       @Local(ordinal = 0) Level level,
                                                       @Local(ordinal = 0) BlockPos pos) throws Throwable {
        MobEffectInstance appliedEffect = arclight$callBeaconEffectEvent(level, pos, player, effect, true);
        if (appliedEffect == null) {
            return false;
        }
        var bridge = (LivingEntityBridge) player;
        var previous = bridge.bridge$getEffectCause().orElse(null);
        bridge.bridge$pushEffectCause(EntityPotionEffectEvent.Cause.BEACON);
        try {
            return (boolean) DecorationOps.callsite().invoke(player, appliedEffect);
        } finally {
            bridge.bridge$pushEffectCause(previous);
        }
    }

    @Decorate(method = "applyEffects(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;ILnet/minecraft/core/Holder;Lnet/minecraft/core/Holder;)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;addEffect(Lnet/minecraft/world/effect/MobEffectInstance;)Z",
            ordinal = 1), require = 1, allow = 1)
    private static boolean arclight$secondaryBeaconEffect(Player player, MobEffectInstance effect,
                                                         @Local(ordinal = 0) Level level,
                                                         @Local(ordinal = 0) BlockPos pos) throws Throwable {
        MobEffectInstance appliedEffect = arclight$callBeaconEffectEvent(level, pos, player, effect, false);
        if (appliedEffect == null) {
            return false;
        }
        var bridge = (LivingEntityBridge) player;
        var previous = bridge.bridge$getEffectCause().orElse(null);
        bridge.bridge$pushEffectCause(EntityPotionEffectEvent.Cause.BEACON);
        try {
            return (boolean) DecorationOps.callsite().invoke(player, appliedEffect);
        } finally {
            bridge.bridge$pushEffectCause(previous);
        }
    }

    @Nullable
    private static MobEffectInstance arclight$callBeaconEffectEvent(Level level, BlockPos pos, Player player,
                                                                   MobEffectInstance effect, boolean primary) {
        BeaconEffectEvent event = new BeaconEffectEvent(CraftBlock.at(level, pos), CraftPotionUtil.toBukkit(effect),
            (org.bukkit.entity.Player) ((LivingEntityBridge) player).bridge$getBukkitEntity(), primary);
        return event.callEvent() ? CraftPotionUtil.fromBukkit(event.getEffect()) : null;
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
