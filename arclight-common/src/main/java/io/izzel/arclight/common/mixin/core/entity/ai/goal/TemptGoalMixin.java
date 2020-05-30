package io.izzel.arclight.common.mixin.core.entity.ai.goal;

import net.minecraft.entity.CreatureEntity;
import net.minecraft.entity.ai.goal.TemptGoal;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import org.bukkit.craftbukkit.v.entity.CraftHumanEntity;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TemptGoal.class)
public abstract class TemptGoalMixin {

    // @formatter:off
    @Shadow protected PlayerEntity closestPlayer;
    @Shadow protected abstract boolean isTempting(ItemStack stack);
    @Shadow @Final protected CreatureEntity creature;
    // @formatter:on

    @Inject(method = "shouldExecute", cancellable = true, at = @At(value = "INVOKE_ASSIGN", ordinal = 0, target = "Lnet/minecraft/world/World;getClosestPlayer(Lnet/minecraft/entity/EntityPredicate;Lnet/minecraft/entity/LivingEntity;)Lnet/minecraft/entity/player/PlayerEntity;"))
    public void arclight$tempt(CallbackInfoReturnable<Boolean> cir) {
        boolean tempt = this.closestPlayer != null && (this.isTempting(this.closestPlayer.getHeldItemMainhand()) || this.isTempting(this.closestPlayer.getHeldItemOffhand()));
        if (tempt) {
            EntityTargetLivingEntityEvent event = CraftEventFactory.callEntityTargetLivingEvent(this.creature, this.closestPlayer, EntityTargetEvent.TargetReason.TEMPT);
            if (event.isCancelled()) {
                cir.setReturnValue(false);
                return;
            }
            this.closestPlayer = (event.getTarget() == null) ? null : ((CraftHumanEntity) event.getTarget()).getHandle();
        }
        cir.setReturnValue(tempt);
    }
}
