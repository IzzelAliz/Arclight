package io.izzel.arclight.common.mixin.core.world.entity.animal.horse;

import io.izzel.arclight.common.bridge.core.inventory.IInventoryBridge;
import io.izzel.arclight.common.mixin.core.world.entity.animal.AnimalMixin;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
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

@Mixin(AbstractHorse.class)
public abstract class AbstractHorseMixin extends AnimalMixin {

    public int maxDomestication;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void arclight$init(EntityType<? extends AbstractHorse> type, Level worldIn, CallbackInfo ci) {
        this.maxDomestication = 100;
    }

    @Redirect(method = "createInventory", at = @At(value = "NEW", target = "net/minecraft/world/SimpleContainer"))
    private SimpleContainer arclight$createInv(int slots) {
        SimpleContainer inventory = new SimpleContainer(slots);
        ((IInventoryBridge) inventory).setOwner((InventoryHolder) this.getBukkitEntity());
        return inventory;
    }

    @Inject(method = "handleEating", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/animal/horse/AbstractHorse;heal(F)V"))
    private void arclight$healByEating(Player player, ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        bridge$pushHealReason(EntityRegainHealthEvent.RegainReason.EATING);
    }

    @Inject(method = "aiStep", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/animal/horse/AbstractHorse;heal(F)V"))
    private void arclight$healByRegen(CallbackInfo ci) {
        bridge$pushHealReason(EntityRegainHealthEvent.RegainReason.REGEN);
    }

    @Inject(method = "addAdditionalSaveData", at = @At("RETURN"))
    private void arclight$writeTemper(CompoundTag compound, CallbackInfo ci) {
        compound.putInt("Bukkit.MaxDomestication", this.maxDomestication);
    }

    @Inject(method = "readAdditionalSaveData", at = @At("RETURN"))
    private void arclight$readTemper(CompoundTag compound, CallbackInfo ci) {
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
        HorseJumpEvent event = CraftEventFactory.callHorseJumpEvent((AbstractHorse) (Object) this, power);
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
