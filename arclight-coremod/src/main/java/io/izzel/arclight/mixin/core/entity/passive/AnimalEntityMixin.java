package io.izzel.arclight.mixin.core.entity.passive;

import io.izzel.arclight.bridge.entity.passive.AnimalEntityBridge;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import io.izzel.arclight.mixin.core.entity.AgeableEntityMixin;

@Mixin(AnimalEntity.class)
public abstract class AnimalEntityMixin extends AgeableEntityMixin implements AnimalEntityBridge {

    // @formatter:off
    @Shadow public boolean processInteract(PlayerEntity player, Hand hand) { return false; }
    // @formatter:on

    public ItemStack breedItem;

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public boolean attackEntityFrom(DamageSource source, float amount) {
        return super.attackEntityFrom(source, amount);
    }

    @Inject(method = "setInLove(Lnet/minecraft/entity/player/PlayerEntity;)V", at = @At("HEAD"))
    private void arclight$setBreedItem(PlayerEntity player, CallbackInfo ci) {
        this.breedItem = player.inventory.getCurrentItem();
    }

    @Override
    public ItemStack bridge$getBreedItem() {
        return breedItem;
    }
}
