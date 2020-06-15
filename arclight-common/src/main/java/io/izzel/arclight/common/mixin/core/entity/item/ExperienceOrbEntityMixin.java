package io.izzel.arclight.common.mixin.core.entity.item;

import io.izzel.arclight.common.mixin.core.entity.EntityMixin;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.item.ExperienceOrbEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerXpEvent;
import org.bukkit.craftbukkit.v.entity.CraftLivingEntity;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;

@Mixin(ExperienceOrbEntity.class)
public abstract class ExperienceOrbEntityMixin extends EntityMixin {

    // @formatter:off
    @Shadow private PlayerEntity closestPlayer;
    @Shadow public abstract boolean attackEntityFrom(DamageSource source, float amount);
    @Shadow public int delayBeforeCanPickup;
    @Shadow public int xpValue;
    @Shadow protected abstract int durabilityToXp(int durability);
    // @formatter:on

    private transient PlayerEntity arclight$lastPlayer;

    @Inject(method = "tick", at = @At(value = "INVOKE", shift = At.Shift.AFTER, target = "Lnet/minecraft/entity/Entity;tick()V"))
    private void arclight$captureLast(CallbackInfo ci) {
        arclight$lastPlayer = this.closestPlayer;
    }

    @Inject(method = "tick", at = @At("RETURN"))
    private void arclight$captureReset(CallbackInfo ci) {
        arclight$lastPlayer = null;
    }

    @Redirect(method = "tick", at = @At(value = "FIELD", ordinal = 6, target = "Lnet/minecraft/entity/item/ExperienceOrbEntity;closestPlayer:Lnet/minecraft/entity/player/PlayerEntity;"))
    private PlayerEntity arclight$targetPlayer(ExperienceOrbEntity entity) {
        if (this.closestPlayer != arclight$lastPlayer) {
            EntityTargetLivingEntityEvent event = CraftEventFactory.callEntityTargetLivingEvent((ExperienceOrbEntity) (Object) this, this.closestPlayer, (this.closestPlayer != null) ? EntityTargetEvent.TargetReason.CLOSEST_PLAYER : EntityTargetEvent.TargetReason.FORGOT_TARGET);
            LivingEntity target = (event.getTarget() == null) ? null : ((CraftLivingEntity) event.getTarget()).getHandle();

            if (event.isCancelled()) {
                this.closestPlayer = arclight$lastPlayer;
                return null;
            } else {
                this.closestPlayer = (target instanceof PlayerEntity) ? (PlayerEntity) target : null;
            }
        }
        return this.closestPlayer;
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public void onCollideWithPlayer(PlayerEntity entityIn) {
        if (!this.world.isRemote) {
            if (this.delayBeforeCanPickup == 0 && entityIn.xpCooldown == 0) {
                if (MinecraftForge.EVENT_BUS.post(new PlayerXpEvent.PickupXp(entityIn, (ExperienceOrbEntity) (Object) this)))
                    return;
                entityIn.xpCooldown = 2;
                entityIn.onItemPickup((ExperienceOrbEntity) (Object) this, 1);
                Map.Entry<EquipmentSlotType, ItemStack> entry = EnchantmentHelper.getRandomItemWithEnchantment(Enchantments.MENDING, entityIn);
                if (entry != null) {
                    ItemStack itemstack = entry.getValue();
                    if (!itemstack.isEmpty() && itemstack.isDamaged()) {
                        int i = Math.min((int) (this.xpValue * itemstack.getXpRepairRatio()), itemstack.getDamage());
                        org.bukkit.event.player.PlayerItemMendEvent event = CraftEventFactory.callPlayerItemMendEvent(entityIn, (ExperienceOrbEntity) (Object) this, itemstack, i);
                        i = event.getRepairAmount();
                        if (!event.isCancelled()) {
                            this.xpValue -= this.durabilityToXp(i);
                            itemstack.setDamage(itemstack.getDamage() - i);
                        }
                    }
                }

                if (this.xpValue > 0) {
                    entityIn.giveExperiencePoints(CraftEventFactory.callPlayerExpChangeEvent(entityIn, this.xpValue).getAmount());
                }

                this.remove();
            }

        }
    }

    @Inject(method = "getXPSplit", cancellable = true, at = @At("HEAD"))
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

    @Override
    public void burn(float amount) {
        this.attackEntityFrom(DamageSource.IN_FIRE, amount);
    }
}
