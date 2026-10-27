package io.izzel.arclight.common.mixin.core.world.entity.monster;

import com.destroystokyo.paper.event.entity.EndermanAttackPlayerEvent;
import com.destroystokyo.paper.event.entity.EndermanEscapeEvent;
import io.izzel.arclight.common.bridge.core.entity.EntityBridge;
import io.izzel.arclight.common.bridge.core.world.entity.monster.EnderManBridge;
import io.izzel.arclight.common.mixin.core.world.entity.PathfinderMobMixin;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import io.izzel.arclight.mixin.Local;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.player.Player;
import org.bukkit.event.entity.EntityTargetEvent;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EnderMan.class)
public abstract class EnderManMixin extends PathfinderMobMixin implements EnderManBridge {

    // @formatter:off
    @Shadow private int targetChangeTime;
    @Shadow @Final private static EntityDataAccessor<Boolean> DATA_CREEPY;
    @Shadow @Final private static EntityDataAccessor<Boolean> DATA_STARED_AT;
    @Shadow @Final private static AttributeModifier SPEED_MODIFIER_ATTACKING;
    @Shadow @Final private static Identifier SPEED_MODIFIER_ATTACKING_ID;
    // @formatter:on

    @Override
    public void bridge$setCreepy(boolean creepy) {
        this.entityData.set(DATA_CREEPY, creepy);
    }

    @Override
    public void bridge$setHasBeenStaredAt(boolean staredAt) {
        this.entityData.set(DATA_STARED_AT, staredAt);
    }

    @Override
    public void bridge$updateTarget(LivingEntity livingEntity) {
        AttributeInstance modifiableattributeinstance = this.getAttribute(Attributes.MOVEMENT_SPEED);
        if (livingEntity == null) {
            this.targetChangeTime = 0;
            this.entityData.set(DATA_CREEPY, false);
            this.entityData.set(DATA_STARED_AT, false);
            modifiableattributeinstance.removeModifier(SPEED_MODIFIER_ATTACKING);
        } else {
            this.targetChangeTime = this.tickCount;
            this.entityData.set(DATA_CREEPY, true);
            if (!modifiableattributeinstance.hasModifier(SPEED_MODIFIER_ATTACKING_ID)) {
                modifiableattributeinstance.addTransientModifier(SPEED_MODIFIER_ATTACKING);
            }
        }
    }

    @Override
    public boolean setTarget(LivingEntity livingEntity, EntityTargetEvent.TargetReason reason, boolean fireEvent) {
        if (!super.setTarget(livingEntity, reason, fireEvent)) {
            return false;
        }
        bridge$updateTarget(getTarget());
        return true;
    }

    @Decorate(method = "isBeingStaredBy(Lnet/minecraft/world/entity/player/Player;)Z", at = @At("RETURN"), require = 1, allow = 2)
    private void arclight$attackPlayer(boolean shouldAttack, Player player) throws Throwable {
        // Use the settled native/loader predicate, including disguise and anger-suppression hooks.
        var event = new EndermanAttackPlayerEvent((org.bukkit.entity.Enderman) this.getBukkitEntity(),
            (org.bukkit.entity.Player) ((EntityBridge) player).bridge$getBukkitEntity());
        event.setCancelled(!shouldAttack);
        DecorationOps.callsite().invoke(event.callEvent());
    }

    @Decorate(method = "customServerAiStep(Lnet/minecraft/server/level/ServerLevel;)V", inject = true,
        at = @At("HEAD"), require = 1, allow = 1)
    private void arclight$initRunaway(@Local(allocate = "arclightRunawayAllowed") boolean allowed) throws Throwable {
        allowed = true;
        DecorationOps.blackhole().invoke(allowed);
    }

    @Decorate(method = "customServerAiStep(Lnet/minecraft/server/level/ServerLevel;)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/monster/EnderMan;setTarget(Lnet/minecraft/world/entity/LivingEntity;)V"),
        require = 1, allow = 1)
    private void arclight$runaway(EnderMan enderman, LivingEntity target,
                                @Local(allocate = "arclightRunawayAllowed") boolean allowed) throws Throwable {
        allowed = new EndermanEscapeEvent((org.bukkit.entity.Enderman) this.getBukkitEntity(), EndermanEscapeEvent.Reason.RUNAWAY).callEvent();
        DecorationOps.blackhole().invoke(allowed);
        if (allowed) {
            DecorationOps.callsite().invoke(enderman, target);
        }
    }

    @Decorate(method = "customServerAiStep(Lnet/minecraft/server/level/ServerLevel;)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/monster/EnderMan;teleport()Z"), require = 1, allow = 1)
    private boolean arclight$runawayTeleport(EnderMan enderman,
                                           @Local(allocate = "arclightRunawayAllowed") boolean allowed) throws Throwable {
        // Veto skips both runaway effects, not the native super.customServerAiStep tail.
        return allowed && (boolean) DecorationOps.callsite().invoke(enderman);
    }

    @Decorate(method = "hurtServer(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;F)Z",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/util/RandomSource;nextInt(I)I"), require = 1, allow = 1)
    private int arclight$damageEscape(RandomSource random, int bound, ServerLevel level, DamageSource source, float damage) throws Throwable {
        int roll = (int) DecorationOps.callsite().invoke(random, bound);
        if (roll != 0) {
            var reason = source.is(DamageTypeTags.IS_DROWNING) ? EndermanEscapeEvent.Reason.DROWN : EndermanEscapeEvent.Reason.CRITICAL_HIT;
            if (!new EndermanEscapeEvent((org.bukkit.entity.Enderman) this.getBukkitEntity(), reason).callEvent()) {
                return 0;
            }
        }
        return roll;
    }

    @Decorate(method = "hurtServer(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;F)Z", inject = true,
        at = @At(value = "JUMP", opcode = Opcodes.IF_ICMPGE), require = 1, allow = 1)
    private void arclight$indirectEscape(@Local(ordinal = 0) boolean hurtWithCleanWater, @Local(ordinal = 0) int attempt) throws Throwable {
        // This is the unique 64-attempt loop check on both loaders; dispatch only at its first entry.
        // Water damage has already run. Veto returns that exact result without adding projectile damage.
        if (attempt == 0 && !new EndermanEscapeEvent((org.bukkit.entity.Enderman) this.getBukkitEntity(), EndermanEscapeEvent.Reason.INDIRECT).callEvent()) {
            DecorationOps.cancel().invoke(hurtWithCleanWater);
            return;
        }
    }

    @Inject(method = "setTarget", cancellable = true, at = @At(value = "INVOKE", shift = At.Shift.AFTER, target = "Lnet/minecraft/world/entity/monster/Monster;setTarget(Lnet/minecraft/world/entity/LivingEntity;)V"))
    private void arclight$returnIfFailed(LivingEntity livingEntity, CallbackInfo ci) {
        if (!arclight$targetSuccess) {
            ci.cancel();
        }
    }

    public void setCreepy(boolean creepy) {
        this.bridge$setCreepy(creepy);
    }

    public void setHasBeenStaredAt(boolean staredAt) {
        this.bridge$setHasBeenStaredAt(staredAt);
    }

}
