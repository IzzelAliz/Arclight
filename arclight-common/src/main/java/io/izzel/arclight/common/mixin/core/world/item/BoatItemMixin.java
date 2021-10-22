package io.izzel.arclight.common.mixin.core.world.item;

import io.izzel.arclight.common.mod.util.DistValidate;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.item.BoatItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.List;
import java.util.function.Predicate;

@Mixin(BoatItem.class)
public class BoatItemMixin extends Item {

    // @formatter:off
    @Shadow @Final private static Predicate<Entity> ENTITY_PREDICATE;
    @Shadow @Final private Boat.Type type;
    // @formatter:on

    public BoatItemMixin(Properties properties) {
        super(properties);
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level worldIn, Player playerIn, @NotNull InteractionHand handIn) {
        ItemStack itemstack = playerIn.getItemInHand(handIn);
        BlockHitResult result = getPlayerPOVHitResult(worldIn, playerIn, ClipContext.Fluid.ANY);
        if (result.getType() == HitResult.Type.MISS) {
            return new InteractionResultHolder<>(InteractionResult.PASS, itemstack);
        } else {
            Vec3 vec3d = playerIn.getViewVector(1.0F);
            double d0 = 5.0D;
            List<Entity> list = worldIn.getEntities(playerIn, playerIn.getBoundingBox().expandTowards(vec3d.scale(5.0D)).inflate(1.0D), ENTITY_PREDICATE);
            if (!list.isEmpty()) {
                Vec3 vec3d1 = playerIn.getEyePosition(1.0F);

                for (Entity entity : list) {
                    AABB axisalignedbb = entity.getBoundingBox().inflate(entity.getPickRadius());
                    if (axisalignedbb.contains(vec3d1)) {
                        return new InteractionResultHolder<>(InteractionResult.PASS, itemstack);
                    }
                }
            }

            if (result.getType() == HitResult.Type.BLOCK) {
                if (DistValidate.isValid(worldIn)) {
                    PlayerInteractEvent event = CraftEventFactory.callPlayerInteractEvent(playerIn, Action.RIGHT_CLICK_BLOCK, result.getBlockPos(), result.getDirection(), itemstack, handIn);

                    if (event.isCancelled()) {
                        return new InteractionResultHolder<>(InteractionResult.PASS, itemstack);
                    }
                }

                Boat boatentity = new Boat(worldIn, result.getLocation().x, result.getLocation().y, result.getLocation().z);
                boatentity.setType(this.type);
                boatentity.setYRot(playerIn.getYRot());
                if (!worldIn.noCollision(boatentity, boatentity.getBoundingBox().inflate(-0.1D))) {
                    return new InteractionResultHolder<>(InteractionResult.FAIL, itemstack);
                } else {
                    if (!worldIn.isClientSide) {
                        if (DistValidate.isValid(worldIn) && CraftEventFactory.callEntityPlaceEvent(worldIn, result.getBlockPos(), result.getDirection(), playerIn, boatentity).isCancelled()) {
                            return new InteractionResultHolder<>(InteractionResult.FAIL, itemstack);
                        }
                        if (!worldIn.addFreshEntity(boatentity)) {
                            return new InteractionResultHolder<>(InteractionResult.PASS, itemstack);
                        }

                        if (!playerIn.getAbilities().instabuild) {
                            itemstack.shrink(1);
                        }
                    }

                    playerIn.awardStat(Stats.ITEM_USED.get(this));
                    return InteractionResultHolder.sidedSuccess(itemstack, worldIn.isClientSide());
                }
            } else {
                return new InteractionResultHolder<>(InteractionResult.PASS, itemstack);
            }
        }
    }
}
