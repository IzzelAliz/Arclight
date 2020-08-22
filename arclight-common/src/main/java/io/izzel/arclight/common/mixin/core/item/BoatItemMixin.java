package io.izzel.arclight.common.mixin.core.item;

import net.minecraft.entity.Entity;
import net.minecraft.entity.item.BoatEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BoatItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.stats.Stats;
import net.minecraft.util.ActionResult;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.RayTraceContext;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
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
    @Shadow @Final private static Predicate<Entity> field_219989_a;
    @Shadow @Final private BoatEntity.Type type;
    // @formatter:on

    public BoatItemMixin(Properties properties) {
        super(properties);
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public @NotNull ActionResult<ItemStack> onItemRightClick(@NotNull World worldIn, PlayerEntity playerIn, @NotNull Hand handIn) {
        ItemStack itemstack = playerIn.getHeldItem(handIn);
        BlockRayTraceResult result = rayTrace(worldIn, playerIn, RayTraceContext.FluidMode.ANY);
        if (result.getType() == RayTraceResult.Type.MISS) {
            return new ActionResult<>(ActionResultType.PASS, itemstack);
        } else {
            Vector3d vec3d = playerIn.getLook(1.0F);
            double d0 = 5.0D;
            List<Entity> list = worldIn.getEntitiesInAABBexcluding(playerIn, playerIn.getBoundingBox().expand(vec3d.scale(5.0D)).grow(1.0D), field_219989_a);
            if (!list.isEmpty()) {
                Vector3d vec3d1 = playerIn.getEyePosition(1.0F);

                for (Entity entity : list) {
                    AxisAlignedBB axisalignedbb = entity.getBoundingBox().grow(entity.getCollisionBorderSize());
                    if (axisalignedbb.contains(vec3d1)) {
                        return new ActionResult<>(ActionResultType.PASS, itemstack);
                    }
                }
            }

            if (result.getType() == RayTraceResult.Type.BLOCK) {
                PlayerInteractEvent event = CraftEventFactory.callPlayerInteractEvent(playerIn, Action.RIGHT_CLICK_BLOCK, result.getPos(), result.getFace(), itemstack, handIn);

                if (event.isCancelled()) {
                    return new ActionResult<>(ActionResultType.PASS, itemstack);
                }

                BoatEntity boatentity = new BoatEntity(worldIn, result.getHitVec().x, result.getHitVec().y, result.getHitVec().z);
                boatentity.setBoatType(this.type);
                boatentity.rotationYaw = playerIn.rotationYaw;
                if (!worldIn.hasNoCollisions(boatentity, boatentity.getBoundingBox().grow(-0.1D))) {
                    return new ActionResult<>(ActionResultType.FAIL, itemstack);
                } else {
                    if (!worldIn.isRemote) {
                        if (CraftEventFactory.callEntityPlaceEvent(worldIn, result.getPos(), result.getFace(), playerIn, boatentity).isCancelled()) {
                            return new ActionResult<>(ActionResultType.FAIL, itemstack);
                        }
                        if (!worldIn.addEntity(boatentity)) {
                            return new ActionResult<>(ActionResultType.PASS, itemstack);
                        }

                        if (!playerIn.abilities.isCreativeMode) {
                            itemstack.shrink(1);
                        }
                    }

                    playerIn.addStat(Stats.ITEM_USED.get(this));
                    return ActionResult.func_233538_a_(itemstack, worldIn.isRemote());
                }
            } else {
                return new ActionResult<>(ActionResultType.PASS, itemstack);
            }
        }
    }
}
