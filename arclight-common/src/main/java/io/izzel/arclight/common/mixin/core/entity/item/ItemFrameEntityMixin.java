package io.izzel.arclight.common.mixin.core.entity.item;

import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.ItemFrameEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.util.DamageSource;
import net.minecraft.util.Direction;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemFrameEntity.class)
public abstract class ItemFrameEntityMixin extends HangingEntityMixin {

    @Shadow @Final private static DataParameter<ItemStack> ITEM;

    @Inject(method = "attackEntityFrom", cancellable = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/item/ItemFrameEntity;dropItemOrSelf(Lnet/minecraft/entity/Entity;Z)V"))
    private void arclight$damageNonLiving(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (CraftEventFactory.handleNonLivingEntityDamageEvent((ItemFrameEntity) (Object) this, source, amount, false) || this.removed) {
            cir.setReturnValue(true);
        }
    }

    public void setItem(ItemStack itemstack, final boolean flag, final boolean playSound) {
        if (!itemstack.isEmpty()) {
            itemstack = itemstack.copy();
            itemstack.setCount(1);
            itemstack.setAttachedEntity((ItemFrameEntity) (Object) this);
        }
        this.getDataManager().set(ITEM, itemstack);
        if (!itemstack.isEmpty() && playSound) {
            this.playSound(SoundEvents.ENTITY_ITEM_FRAME_ADD_ITEM, 1.0f, 1.0f);
        }
        if (flag && this.hangingPosition != null) {
            this.world.updateComparatorOutputLevel(this.hangingPosition, Blocks.AIR);
        }
    }

    private static AxisAlignedBB calculateBoundingBox(Entity entity, BlockPos blockPosition, Direction direction, int width, int height) {
        double d0 = 0.46875;
        double locX = blockPosition.getX() + 0.5 - direction.getXOffset() * 0.46875;
        double locY = blockPosition.getY() + 0.5 - direction.getYOffset() * 0.46875;
        double locZ = blockPosition.getZ() + 0.5 - direction.getZOffset() * 0.46875;
        if (entity != null) {
            entity.setRawPosition(locX, locY, locZ);
        }
        double d2 = width;
        double d3 = height;
        double d4 = width;
        Direction.Axis enumdirection_enumaxis = direction.getAxis();
        switch (enumdirection_enumaxis) {
            case X: {
                d2 = 1.0;
                break;
            }
            case Y: {
                d3 = 1.0;
                break;
            }
            case Z: {
                d4 = 1.0;
                break;
            }
        }
        d2 /= 32.0;
        d3 /= 32.0;
        d4 /= 32.0;
        return new AxisAlignedBB(locX - d2, locY - d3, locZ - d4, locX + d2, locY + d3, locZ + d4);
    }
}
