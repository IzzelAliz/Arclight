package io.izzel.arclight.common.mixin.core.item;

import io.izzel.arclight.common.bridge.entity.player.ServerPlayerEntityBridge;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.entity.projectile.SnowballEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SnowballItem;
import net.minecraft.stats.Stats;
import net.minecraft.util.ActionResult;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(SnowballItem.class)
public class SnowballItemMixin extends Item {

    public SnowballItemMixin(Properties properties) {
        super(properties);
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public @NotNull ActionResult<ItemStack> onItemRightClick(World worldIn, PlayerEntity playerIn, @NotNull Hand handIn) {
        ItemStack itemstack = playerIn.getHeldItem(handIn);
        if (!worldIn.isRemote) {
            SnowballEntity snowballentity = new SnowballEntity(worldIn, playerIn);
            snowballentity.setItem(itemstack);
            if (worldIn.addEntity(snowballentity)) {
                if (!playerIn.abilities.isCreativeMode) {
                    itemstack.shrink(1);
                }
                worldIn.playSound(null, playerIn.getPosX(), playerIn.getPosY(), playerIn.getPosZ(), SoundEvents.ENTITY_SNOWBALL_THROW, SoundCategory.NEUTRAL, 0.5F, 0.4F / (random.nextFloat() * 0.4F + 0.8F));
                snowballentity.func_234612_a_(playerIn, playerIn.rotationPitch, playerIn.rotationYaw, 0.0F, 1.5F, 1.0F);
            } else if (playerIn instanceof ServerPlayerEntity) {
                ((ServerPlayerEntityBridge) playerIn).bridge$getBukkitEntity().updateInventory();
            }
        }

        playerIn.addStat(Stats.ITEM_USED.get(this));
        return new ActionResult<>(ActionResultType.SUCCESS, itemstack);
    }
}
