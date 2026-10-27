package io.izzel.arclight.forge.mixin.core.world.entity.item;

import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ItemEntity.class)
public abstract class ItemEntityMixin_Forge {
    @org.spongepowered.asm.mixin.Shadow(remap = false) public int lifespan;
    @org.spongepowered.asm.mixin.Shadow public int age;

    @com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod(method = "arclight$inactiveItemExpired", remap = false)
    private boolean arclight$inactiveExpiry(com.llamalad7.mixinextras.injector.wrapoperation.Operation<Boolean> original) {
        if (this.age < this.lifespan) return false;
        ItemEntity item = (ItemEntity) (Object) this;
        int extra = net.minecraftforge.event.ForgeEventFactory.onItemExpire(item, item.getItem());
        if (extra < 0) return true;
        this.lifespan += extra;
        return false;
    }

}
