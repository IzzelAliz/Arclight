package io.izzel.arclight.impl.mixin.optimization.general.activationrange.entity;

import io.izzel.arclight.common.bridge.world.WorldBridge;
import io.izzel.arclight.common.mod.ArclightConstants;
import io.izzel.arclight.impl.mixin.optimization.general.activationrange.EntityMixin_ActivationRange;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.event.ForgeEventFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemEntity.class)
public abstract class ItemEntityMixin_ActivationRange extends EntityMixin_ActivationRange {

    // @formatter:off
    @Shadow public int pickupDelay;
    @Shadow public int age;
    @Shadow(remap = false) public int lifespan;
    @Shadow public abstract ItemStack getItem();
    // @formatter:on

    @Inject(method = "<init>(Lnet/minecraft/entity/EntityType;Lnet/minecraft/world/World;)V", at = @At("RETURN"))
    private void activationRange$init(EntityType<? extends ItemEntity> entityType, World world, CallbackInfo ci) {
        this.lifespan = ((WorldBridge) this.world).bridge$spigotConfig().itemDespawnRate;
    }

    @Inject(method = "<init>(Lnet/minecraft/world/World;DDDLnet/minecraft/item/ItemStack;)V", at = @At("RETURN"))
    private void activationRange$init(World worldIn, double x, double y, double z, ItemStack stack, CallbackInfo ci) {
        if (this.lifespan == 6000) {
            this.lifespan = ((WorldBridge) this.world).bridge$spigotConfig().itemDespawnRate;
        }
    }

    private int lastTick = ArclightConstants.currentTick - 1;

    @Override
    public void inactiveTick() {
        super.inactiveTick();
        int elapsedTicks = ArclightConstants.currentTick - this.lastTick;
        if (this.pickupDelay > 0 && this.pickupDelay != 32767 && elapsedTicks > 0) this.pickupDelay -= elapsedTicks;
        if (this.age != -32768) this.age += elapsedTicks;
        this.lastTick = ArclightConstants.currentTick;

        if (!this.world.isRemote && this.age >= this.lifespan) {
            int hook = ForgeEventFactory.onItemExpire((ItemEntity) (Object) this, this.getItem());
            if (hook < 0) this.remove();
            else this.lifespan += hook;
        }
    }
}
