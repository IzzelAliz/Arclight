package io.izzel.arclight.common.mixin.core.world.item;

import io.izzel.arclight.common.bridge.core.entity.player.ServerPlayerEntityBridge;
import io.izzel.arclight.common.mod.util.DistValidate;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Fox;
import net.minecraft.world.item.ChorusFruitItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
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
    public @NotNull ItemStack finishUsingItem(@NotNull ItemStack stack, @NotNull Level worldIn, @NotNull LivingEntity entityLiving) {
        ItemStack itemstack = super.finishUsingItem(stack, worldIn, entityLiving);
        if (!worldIn.isClientSide) {
            double d0 = entityLiving.getX();
            double d1 = entityLiving.getY();
            double d2 = entityLiving.getZ();

            for (int i = 0; i < 16; ++i) {
                double d3 = entityLiving.getX() + (entityLiving.getRandom().nextDouble() - 0.5D) * 16.0D;
                double d4 = Mth.clamp(entityLiving.getY() + (double) (entityLiving.getRandom().nextInt(16) - 8), 0.0D, worldIn.getHeight() - 1);
                double d5 = entityLiving.getZ() + (entityLiving.getRandom().nextDouble() - 0.5D) * 16.0D;

                if (entityLiving instanceof ServerPlayer && DistValidate.isValid(worldIn)) {
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

                if (entityLiving.randomTeleport(d3, d4, d5, true)) {
                    SoundEvent soundevent = entityLiving instanceof Fox ? SoundEvents.FOX_TELEPORT : SoundEvents.CHORUS_FRUIT_TELEPORT;
                    worldIn.playSound(null, d0, d1, d2, soundevent, SoundSource.PLAYERS, 1.0F, 1.0F);
                    entityLiving.playSound(soundevent, 1.0F, 1.0F);
                    break;
                }
            }

            if (entityLiving instanceof net.minecraft.world.entity.player.Player) {
                ((net.minecraft.world.entity.player.Player) entityLiving).getCooldowns().addCooldown(this, 20);
            }
        }

        return itemstack;
    }
}
