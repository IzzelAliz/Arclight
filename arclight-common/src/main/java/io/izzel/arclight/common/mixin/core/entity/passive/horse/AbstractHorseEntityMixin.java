package io.izzel.arclight.common.mixin.core.entity.passive.horse;

import io.izzel.arclight.common.bridge.inventory.IInventoryBridge;
import io.izzel.arclight.common.mixin.core.entity.passive.AnimalEntityMixin;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.horse.AbstractHorseEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.world.World;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.HorseJumpEvent;
import org.bukkit.inventory.InventoryHolder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractHorseEntity.class)
public abstract class AbstractHorseEntityMixin extends AnimalEntityMixin {

    public int maxDomestication;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void arclight$init(EntityType<? extends AbstractHorseEntity> type, World worldIn, CallbackInfo ci) {
        this.maxDomestication = 100;
    }

    @Redirect(method = "initHorseChest", at = @At(value = "NEW", target = "net/minecraft/inventory/Inventory"))
    private Inventory arclight$createInv(int slots) {
        Inventory inventory = new Inventory(slots);
        ((IInventoryBridge) inventory).setOwner((InventoryHolder) this.getBukkitEntity());
        return inventory;
    }

    @Inject(method = "handleEating", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/passive/horse/AbstractHorseEntity;heal(F)V"))
    private void arclight$healByEating(PlayerEntity player, ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        bridge$pushHealReason(EntityRegainHealthEvent.RegainReason.EATING);
    }

    @Inject(method = "livingTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/passive/horse/AbstractHorseEntity;heal(F)V"))
    private void arclight$healByRegen(CallbackInfo ci) {
        bridge$pushHealReason(EntityRegainHealthEvent.RegainReason.REGEN);
    }

    @Inject(method = "writeAdditional", at = @At("RETURN"))
    private void arclight$writeTemper(CompoundNBT compound, CallbackInfo ci) {
        compound.putInt("Bukkit.MaxDomestication", this.maxDomestication);
    }

    @Inject(method = "readAdditional", at = @At("RETURN"))
    private void arclight$readTemper(CompoundNBT compound, CallbackInfo ci) {
        if (compound.contains("Bukkit.MaxDomestication")) {
            this.maxDomestication = compound.getInt("Bukkit.MaxDomestication");
        }
    }

    @Inject(method = "handleStartJump", cancellable = true, at = @At("HEAD"))
    private void arclight$horseJump(int i, CallbackInfo ci) {
        float power;
        if (i >= 90) {
            power = 1.0F;
        } else {
            power = 0.4F + 0.4F * (float) i / 90.0F;
        }
        HorseJumpEvent event = CraftEventFactory.callHorseJumpEvent((AbstractHorseEntity) (Object) this, power);
        if (event.isCancelled()) {
            ci.cancel();
        }
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public int getMaxTemper() {
        return maxDomestication;
    }
}
