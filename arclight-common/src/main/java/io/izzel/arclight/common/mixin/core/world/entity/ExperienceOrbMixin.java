package io.izzel.arclight.common.mixin.core.world.entity;

import com.destroystokyo.paper.event.entity.ExperienceOrbMergeEvent;
import com.destroystokyo.paper.event.player.PlayerPickupExperienceEvent;
import io.izzel.arclight.common.bridge.core.entity.EntityBridge;
import io.izzel.arclight.common.bridge.core.server.level.ServerPlayerBridge;
import io.izzel.arclight.common.bridge.core.world.entity.ExperienceOrbBridge;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import io.izzel.arclight.mixin.Local;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantedItemInUse;
import org.bukkit.craftbukkit.entity.CraftLivingEntity;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.bukkit.event.entity.EntityRemoveEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.player.PlayerExpCooldownChangeEvent;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(ExperienceOrb.class)
public abstract class ExperienceOrbMixin extends EntityMixin implements ExperienceOrbBridge {

    // @formatter:off
    @Shadow private Player followingPlayer;
    @Shadow public abstract void setValue(int value);
    // @formatter:on

    private transient Player arclight$lastPlayer;

    @Override
    public void bridge$setValue(int value) {
        this.setValue(value);
    }

    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/ExperienceOrb;discard()V"))
    private void arclight$tickDespawn(CallbackInfo ci) {
        this.bridge$pushEntityRemoveCause(EntityRemoveEvent.Cause.DESPAWN);
    }

    @Decorate(method = "merge(Lnet/minecraft/world/entity/ExperienceOrb;)V", inject = true, at = @At("HEAD"), require = 1, allow = 1)
    private void arclight$mergeExperience(ExperienceOrb orb) throws Throwable {
        // Admission-time merging already dispatches in CraftEventFactory; this is the native scan path.
        if (!new ExperienceOrbMergeEvent((org.bukkit.entity.ExperienceOrb) this.getBukkitEntity(),
            (org.bukkit.entity.ExperienceOrb) ((EntityBridge) orb).bridge$getBukkitEntity()).callEvent()) {
            DecorationOps.cancel().invoke();
            return;
        }
    }

    @Inject(method = "merge", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/ExperienceOrb;discard()V"))
    private void arclight$merge(ExperienceOrb orb, CallbackInfo ci) {
        ((EntityBridge) orb).bridge$pushEntityRemoveCause(EntityRemoveEvent.Cause.MERGE);
    }

    @Inject(method = "playerTouch", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/ExperienceOrb;discard()V"))
    private void arclight$pickup(CallbackInfo ci) {
        this.bridge$pushEntityRemoveCause(EntityRemoveEvent.Cause.PICKUP);
    }

    @Inject(method = "followNearbyPlayer", at = @At("HEAD"))
    private void arclight$captureLast(CallbackInfo ci) {
        arclight$lastPlayer = this.followingPlayer;
    }

    @Inject(method = "tick", at = @At("RETURN"))
    private void arclight$captureReset(CallbackInfo ci) {
        arclight$lastPlayer = null;
    }

    // The first three reads validate the previous target; the fourth gates attraction.
    @Inject(method = "followNearbyPlayer", cancellable = true, at = @At(value = "FIELD", target = "Lnet/minecraft/world/entity/ExperienceOrb;followingPlayer:Lnet/minecraft/world/entity/player/Player;", opcode = Opcodes.GETFIELD, ordinal = 3))
    private void arclight$targetPlayer(CallbackInfo ci) {
        if (this.followingPlayer != arclight$lastPlayer) {
            EntityTargetLivingEntityEvent event = CraftEventFactory.callEntityTargetLivingEvent((ExperienceOrb) (Object) this, this.followingPlayer, (this.followingPlayer != null) ? EntityTargetEvent.TargetReason.CLOSEST_PLAYER : EntityTargetEvent.TargetReason.FORGOT_TARGET);
            LivingEntity target = (event.getTarget() == null) ? null : ((CraftLivingEntity) event.getTarget()).getHandle();

            if (event.isCancelled()) {
                this.followingPlayer = arclight$lastPlayer;
                ci.cancel();
            } else {
                this.followingPlayer = (target instanceof Player) ? (Player) target : null;
            }
        }
    }

    @Decorate(method = "playerTouch", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;giveExperiencePoints(I)V"))
    private void arclight$expChange(Player player, int amount) throws Throwable {
        DecorationOps.callsite().invoke(player, CraftEventFactory.callPlayerExpChangeEvent(player, (ExperienceOrb) (Object) this, amount).getAmount());
    }

    @Decorate(method = "playerTouch(Lnet/minecraft/world/entity/player/Player;)V", at = @At(value = "FIELD", opcode = Opcodes.PUTFIELD, target = "Lnet/minecraft/world/entity/player/Player;takeXpDelay:I"), require = 1, allow = 1)
    private void arclight$cooldown(Player instance, int value) throws Throwable {
        // Native cooldown/type checks and NeoForge's pickup veto precede this write.
        if (!new PlayerPickupExperienceEvent(((ServerPlayerBridge) instance).bridge$getBukkitEntity(),
            (org.bukkit.entity.ExperienceOrb) this.getBukkitEntity()).callEvent()) {
            DecorationOps.cancel().invoke();
            return;
        }
        DecorationOps.callsite().invoke(instance, CraftEventFactory.callPlayerXpCooldownEvent((ServerPlayer) instance, value, PlayerExpCooldownChangeEvent.ChangeReason.PICKUP_ORB).getNewCooldown());
    }

    @Decorate(method = "repairPlayerItems", inject = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;setDamageValue(I)V"))
    private void arclight$itemMend(ServerPlayer serverPlayer, int i, @Local(ordinal = -1) ItemStack itemstack,
                                   @Local(ordinal = -1) Optional<EnchantedItemInUse> optional, @Local(ordinal = -2) int j, @Local(ordinal = -1) int k) throws Throwable {
        var slot = optional.get().inSlot();
        // https://github.com/IzzelAliz/Arclight/issues/2155
        // Bukkit cannot represent mod-only slots (e.g. Accessories uses ordinal -1).
        // Skip only the Bukkit event; keep native mending and XP consumption intact.
        if (slot != EquipmentSlot.MAINHAND && slot != EquipmentSlot.OFFHAND
            && slot != EquipmentSlot.FEET && slot != EquipmentSlot.LEGS
            && slot != EquipmentSlot.CHEST && slot != EquipmentSlot.HEAD
            && slot != EquipmentSlot.BODY && slot != EquipmentSlot.SADDLE) {
            return;
        }
        org.bukkit.event.player.PlayerItemMendEvent event = CraftEventFactory.callPlayerItemMendEvent(serverPlayer, (ExperienceOrb) (Object) this, itemstack, slot, k, k > 0 ? k * i / j : 0);
        k = event.getRepairAmount();
        if (event.isCancelled()) {
            DecorationOps.cancel().invoke(i);
            return;
        }
        DecorationOps.blackhole().invoke(k);
    }

    @Decorate(method = "repairPlayerItems", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/ExperienceOrb;repairPlayerItems(Lnet/minecraft/server/level/ServerPlayer;I)I"))
    private int arclight$updateXp(ExperienceOrb instance, ServerPlayer serverPlayer, int i) throws Throwable {
        this.setValue(i);
        return (int) DecorationOps.callsite().invoke(instance, serverPlayer, i);
    }

    @Inject(method = "getExperienceValue", cancellable = true, at = @At("HEAD"))
    private static void arclight$higherLevelSplit(int expValue, CallbackInfoReturnable<Integer> cir) {
        // @formatter:off
        if (expValue > 162670129) { cir.setReturnValue(expValue - 100000); return; }
        if (expValue > 81335063) { cir.setReturnValue(81335063); return; }
        if (expValue > 40667527) { cir.setReturnValue(40667527); return; }
        if (expValue > 20333759) { cir.setReturnValue(20333759); return; }
        if (expValue > 10166857) { cir.setReturnValue(10166857); return; }
        if (expValue > 5083423) { cir.setReturnValue(5083423); return; }
        if (expValue > 2541701) { cir.setReturnValue(2541701); return; }
        if (expValue > 1270849) { cir.setReturnValue(1270849); return; }
        if (expValue > 635413) { cir.setReturnValue(635413); return; }
        if (expValue > 317701) { cir.setReturnValue(317701); return; }
        if (expValue > 158849) { cir.setReturnValue(158849); return; }
        if (expValue > 79423) { cir.setReturnValue(79423); return; }
        if (expValue > 39709) { cir.setReturnValue(39709); return; }
        if (expValue > 19853) { cir.setReturnValue(19853); return; }
        if (expValue > 9923) { cir.setReturnValue(9923); return; }
        if (expValue > 4957) { cir.setReturnValue(4957); }
        // @formatter:on
    }
}
