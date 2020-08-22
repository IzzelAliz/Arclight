package io.izzel.arclight.common.mixin.core.item;

import io.izzel.arclight.common.bridge.entity.player.ServerPlayerEntityBridge;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.FoxEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ChorusFruitItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(ChorusFruitItem.class)
public class ChorusFruitItemMixin extends Item {

    public ChorusFruitItemMixin(Properties properties) {
        super(properties);
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public @NotNull ItemStack onItemUseFinish(@NotNull ItemStack stack, @NotNull World worldIn, @NotNull LivingEntity entityLiving) {
        ItemStack itemstack = super.onItemUseFinish(stack, worldIn, entityLiving);
        if (!worldIn.isRemote) {
            double d0 = entityLiving.getPosX();
            double d1 = entityLiving.getPosY();
            double d2 = entityLiving.getPosZ();

            for (int i = 0; i < 16; ++i) {
                double d3 = entityLiving.getPosX() + (entityLiving.getRNG().nextDouble() - 0.5D) * 16.0D;
                double d4 = MathHelper.clamp(entityLiving.getPosY() + (double) (entityLiving.getRNG().nextInt(16) - 8), 0.0D, worldIn.func_234938_ad_() - 1);
                double d5 = entityLiving.getPosZ() + (entityLiving.getRNG().nextDouble() - 0.5D) * 16.0D;

                if (entityLiving instanceof ServerPlayerEntity) {
                    Player player = ((ServerPlayerEntityBridge) entityLiving).bridge$getBukkitEntity();
                    PlayerTeleportEvent event = new PlayerTeleportEvent(player, player.getLocation(), new Location(player.getWorld(), d3, d4, d5), PlayerTeleportEvent.TeleportCause.CHORUS_FRUIT);
                    Bukkit.getPluginManager().callEvent(event);
                    if (event.isCancelled()) {
                        break;
                    }
                    d3 = event.getTo().getX();
                    d4 = event.getTo().getY();
                    d5 = event.getTo().getZ();
                }

                if (entityLiving.isPassenger()) {
                    entityLiving.stopRiding();
                }

                if (entityLiving.attemptTeleport(d3, d4, d5, true)) {
                    SoundEvent soundevent = entityLiving instanceof FoxEntity ? SoundEvents.ENTITY_FOX_TELEPORT : SoundEvents.ITEM_CHORUS_FRUIT_TELEPORT;
                    worldIn.playSound(null, d0, d1, d2, soundevent, SoundCategory.PLAYERS, 1.0F, 1.0F);
                    entityLiving.playSound(soundevent, 1.0F, 1.0F);
                    break;
                }
            }

            if (entityLiving instanceof PlayerEntity) {
                ((PlayerEntity) entityLiving).getCooldownTracker().setCooldown(this, 20);
            }
        }

        return itemstack;
    }
}
