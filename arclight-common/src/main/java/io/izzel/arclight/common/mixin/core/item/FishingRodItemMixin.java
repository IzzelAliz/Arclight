package io.izzel.arclight.common.mixin.core.item;

import io.izzel.arclight.common.bridge.entity.EntityBridge;
import io.izzel.arclight.common.bridge.entity.player.ServerPlayerEntityBridge;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.FishingBobberEntity;
import net.minecraft.item.FishingRodItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.stats.Stats;
import net.minecraft.util.ActionResult;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.world.World;
import org.bukkit.Bukkit;
import org.bukkit.entity.FishHook;
import org.bukkit.event.player.PlayerFishEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(FishingRodItem.class)
public class FishingRodItemMixin extends Item {

    public FishingRodItemMixin(Properties properties) {
        super(properties);
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public ActionResult<ItemStack> onItemRightClick(World worldIn, PlayerEntity playerIn, Hand handIn) {
        ItemStack itemstack = playerIn.getHeldItem(handIn);
        if (playerIn.fishingBobber != null) {
            if (!worldIn.isRemote) {
                int i = playerIn.fishingBobber.handleHookRetraction(itemstack);
                itemstack.damageItem(i, playerIn, (player) -> {
                    player.sendBreakAnimation(handIn);
                });
            }

            playerIn.swingArm(handIn);
            worldIn.playSound(null, playerIn.getPosX(), playerIn.getPosY(), playerIn.getPosZ(), SoundEvents.ENTITY_FISHING_BOBBER_RETRIEVE, SoundCategory.NEUTRAL, 1.0F, 0.4F / (random.nextFloat() * 0.4F + 0.8F));
        } else {
            if (!worldIn.isRemote) {
                int k = EnchantmentHelper.getFishingSpeedBonus(itemstack);
                int j = EnchantmentHelper.getFishingLuckBonus(itemstack);

                FishingBobberEntity hook = new FishingBobberEntity(playerIn, worldIn, j, k);
                PlayerFishEvent playerFishEvent = new PlayerFishEvent(((ServerPlayerEntityBridge) playerIn).bridge$getBukkitEntity(), null, (FishHook) ((EntityBridge) hook).bridge$getBukkitEntity(), PlayerFishEvent.State.FISHING);
                Bukkit.getPluginManager().callEvent(playerFishEvent);

                if (playerFishEvent.isCancelled()) {
                    playerIn.fishingBobber = null;
                    return new ActionResult<>(ActionResultType.PASS, itemstack);
                }
                worldIn.playSound(null, playerIn.getPosX(), playerIn.getPosY(), playerIn.getPosZ(), SoundEvents.ENTITY_FISHING_BOBBER_THROW, SoundCategory.NEUTRAL, 0.5F, 0.4F / (random.nextFloat() * 0.4F + 0.8F));
                worldIn.addEntity(new FishingBobberEntity(playerIn, worldIn, j, k));
            }

            // playerIn.swingArm(handIn);
            playerIn.addStat(Stats.ITEM_USED.get(this));
        }

        return ActionResult.func_233538_a_(itemstack, worldIn.isRemote());
    }
}
