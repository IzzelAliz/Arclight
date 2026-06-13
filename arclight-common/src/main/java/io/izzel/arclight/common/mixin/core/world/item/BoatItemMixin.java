package io.izzel.arclight.common.mixin.core.world.item;

import io.izzel.arclight.common.mod.util.DistValidate;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import io.izzel.arclight.mixin.Local;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.item.BoatItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import java.util.function.Predicate;

@Mixin(BoatItem.class)
public abstract class BoatItemMixin extends Item {

    // @formatter:off
    @Shadow @Final private EntityType<? extends AbstractBoat> entityType;
    // @formatter:on

    public BoatItemMixin(Properties properties) {
        super(properties);
    }

    @Decorate(method = "use", inject = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/BoatItem;getBoat(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/phys/HitResult;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/player/Player;)Lnet/minecraft/world/entity/vehicle/boat/AbstractBoat;"))
    private void arclight$interact(Level level, Player player, InteractionHand interactionHand,
                                   @Local(ordinal = 0) ItemStack itemstack, @Local(ordinal = 0) HitResult result) throws Throwable {
        if (DistValidate.isValid(level)) {
            PlayerInteractEvent event = CraftEventFactory.callPlayerInteractEvent(player, Action.RIGHT_CLICK_BLOCK, ((BlockHitResult) result).getBlockPos(), ((BlockHitResult) result).getDirection(), itemstack, false, interactionHand, result.getLocation());
            if (event.isCancelled()) {
                DecorationOps.cancel().invoke(InteractionResult.PASS);
                return;
            }
        }
        DecorationOps.blackhole().invoke();
    }

    @Decorate(method = "use", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"))
    private boolean arclight$entityPlace(Level instance, Entity entity, @NotNull Level worldIn, Player playerIn, @NotNull InteractionHand handIn,
                                         @Local(ordinal = 0) ItemStack itemstack, @Local(ordinal = 0) HitResult result) throws Throwable {
        if (DistValidate.isValid(worldIn) && CraftEventFactory.callEntityPlaceEvent(worldIn, ((BlockHitResult) result).getBlockPos(), ((BlockHitResult) result).getDirection(), playerIn, entity, handIn).isCancelled()) {
            return (boolean) DecorationOps.cancel().invoke(InteractionResult.FAIL);
        }
        if (!(boolean) DecorationOps.callsite().invoke(instance, entity)) {
            return (boolean) DecorationOps.cancel().invoke(InteractionResult.PASS);
        }
        return true;
    }
}
