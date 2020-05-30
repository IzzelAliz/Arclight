package io.izzel.arclight.common.mixin.core.block;

import io.izzel.arclight.common.bridge.entity.EntityBridge;
import net.minecraft.block.WeightedPressurePlateBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v.block.CraftBlock;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.bukkit.event.Cancellable;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityInteractEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;

@Mixin(WeightedPressurePlateBlock.class)
public class WeightedPressurePlateBlockMixin {

    @Redirect(method = "computeRedstoneStrength", at = @At(value = "INVOKE", remap = false, target = "Ljava/util/List;size()I"))
    public int arclight$entityInteract(List<Entity> list, World worldIn, BlockPos pos) {
        int i = 0;
        for (Entity entity : list) {
            Cancellable cancellable;

            if (entity instanceof PlayerEntity) {
                cancellable = CraftEventFactory.callPlayerInteractEvent((PlayerEntity) entity, Action.PHYSICAL, pos, null, null, null);
            } else {
                cancellable = new EntityInteractEvent(((EntityBridge) entity).bridge$getBukkitEntity(), CraftBlock.at(worldIn, pos));
                Bukkit.getPluginManager().callEvent((EntityInteractEvent) cancellable);
            }

            // We only want to block turning the plate on if all events are cancelled
            if (!cancellable.isCancelled()) {
                i++;
            }
        }
        return i;
    }
}
