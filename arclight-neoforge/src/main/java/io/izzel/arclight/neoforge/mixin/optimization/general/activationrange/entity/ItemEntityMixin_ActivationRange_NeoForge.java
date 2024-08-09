package io.izzel.arclight.neoforge.mixin.optimization.general.activationrange.entity;

import io.izzel.arclight.common.bridge.core.entity.item.ItemEntityBridge;
import io.izzel.arclight.common.bridge.core.world.WorldBridge;
import io.izzel.arclight.common.mod.util.DistValidate;
import io.izzel.arclight.neoforge.mixin.core.world.entity.EntityMixin_NeoForge;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.EventHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemEntity.class)
public abstract class ItemEntityMixin_ActivationRange_NeoForge extends EntityMixin_NeoForge implements ItemEntityBridge {

    // @formatter:off
    @Shadow(remap = false) public int lifespan;
    @Shadow public int age;
    @Shadow public abstract ItemStack getItem();
    // @formatter:on

    @Inject(method = "<init>(Lnet/minecraft/world/entity/EntityType;Lnet/minecraft/world/level/Level;)V", at = @At("RETURN"))
    private void activationRange$init(EntityType<? extends ItemEntity> entityType, Level world, CallbackInfo ci) {
        if (DistValidate.isValid(this.level())) {
            this.lifespan = ((WorldBridge) this.level()).bridge$spigotConfig().itemDespawnRate;
        }
    }

    @Inject(method = "<init>(Lnet/minecraft/world/level/Level;DDDLnet/minecraft/world/item/ItemStack;)V", at = @At("RETURN"))
    private void activationRange$init(Level worldIn, double x, double y, double z, ItemStack stack, CallbackInfo ci) {
        if (DistValidate.isValid(this.level()) && this.lifespan == 6000) {
            this.lifespan = ((WorldBridge) this.level()).bridge$spigotConfig().itemDespawnRate;
        }
    }

    @Override
    public void bridge$forge$optimization$discardItemEntity() {
        if (!this.level().isClientSide && this.age >= this.lifespan) {
            int hook = EventHooks.onItemExpire((ItemEntity) (Object) this);
            if (hook < 0) this.discard();
            else this.lifespan += hook;
        }
    }
}
