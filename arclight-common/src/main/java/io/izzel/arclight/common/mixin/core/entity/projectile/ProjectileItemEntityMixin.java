package io.izzel.arclight.common.mixin.core.entity.projectile;

import io.izzel.arclight.common.bridge.entity.projectile.ProjectileItemEntityBridge;
import net.minecraft.entity.projectile.ProjectileItemEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.util.Util;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Implements;
import org.spongepowered.asm.mixin.Interface;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ProjectileItemEntity.class)
@Implements(@Interface(iface = ProjectileItemEntityBridge.Hack.class, prefix = "hack$"))
public abstract class ProjectileItemEntityMixin extends ThrowableEntityMixin {

    // @formatter:off
    @Shadow protected abstract Item shadow$getDefaultItem();
    @Shadow @Final private static DataParameter<ItemStack> ITEMSTACK_DATA;
    // @formatter:on

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public void setItem(ItemStack stack) {
        if (stack.getItem() != this.shadow$getDefaultItem() || stack.hasTag()) {
            this.getDataManager().set(ITEMSTACK_DATA, Util.make(stack.copy(), (itemStack) -> {
                if (!itemStack.isEmpty()) {
                    itemStack.setCount(1);
                }
            }));
        }

    }

    public Item hack$getDefaultItem() {
        return this.shadow$getDefaultItem();
    }
}
