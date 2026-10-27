package io.izzel.arclight.neoforge.mixin.core.world.entity.item;

import io.izzel.arclight.common.bridge.core.world.level.WorldBridge;
import io.izzel.arclight.common.mod.util.DistValidate;
import io.izzel.arclight.neoforge.mixin.core.world.entity.EntityMixin_NeoForge;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemEntity.class)
public abstract class ItemEntityMixin_NeoForge extends EntityMixin_NeoForge {

    @Shadow(remap = false) public int lifespan;
    @Shadow public int age;

    @com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod(method = "arclight$inactiveItemExpired", remap = false)
    private boolean arclight$inactiveExpiry(com.llamalad7.mixinextras.injector.wrapoperation.Operation<Boolean> original) {
        if (this.age < this.lifespan) return false;
        this.lifespan = net.minecraft.util.Mth.clamp(this.lifespan + net.neoforged.neoforge.event.EventHooks.onItemExpire((ItemEntity) (Object) this), 0, 32766);
        return this.age >= this.lifespan;
    }


    @Inject(method = "<init>(Lnet/minecraft/world/entity/EntityType;Lnet/minecraft/world/level/Level;)V", at = @At("RETURN"))
    private void arclight$initLifespan(EntityType<? extends ItemEntity> entityType, Level world, CallbackInfo ci) {
        if (DistValidate.isValid(this.level())) {
            this.lifespan = ((WorldBridge) this.level()).bridge$spigotConfig().itemDespawnRate;
        }
    }

    @Inject(method = "<init>(Lnet/minecraft/world/level/Level;DDDLnet/minecraft/world/item/ItemStack;)V", at = @At("RETURN"))
    private void arclight$initLifespan(Level world, double x, double y, double z, ItemStack stack, CallbackInfo ci) {
        if (DistValidate.isValid(this.level()) && this.lifespan == 6000) {
            this.lifespan = ((WorldBridge) this.level()).bridge$spigotConfig().itemDespawnRate;
        }
    }
}
