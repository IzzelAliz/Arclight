package io.izzel.arclight.common.mixin.core.world.entity.projectile;

import net.minecraft.Util;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ThrowableItemProjectile.class)
public abstract class ThrowableItemProjectileMixin extends ThrowableProjectileMixin {

    // @formatter:off
    @Shadow protected abstract Item getDefaultItem();
    @Shadow @Final private static EntityDataAccessor<ItemStack> DATA_ITEM_STACK;
    // @formatter:on

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public void setItem(ItemStack stack) {
        if (stack.getItem() != this.getDefaultItem() || stack.hasTag()) {
            this.getEntityData().set(DATA_ITEM_STACK, Util.make(stack.copy(), (itemStack) -> {
                if (!itemStack.isEmpty()) {
                    itemStack.setCount(1);
                }
            }));
        }

    }

    public Item getDefaultItemPublic() {
        return this.getDefaultItem();
    }
}
