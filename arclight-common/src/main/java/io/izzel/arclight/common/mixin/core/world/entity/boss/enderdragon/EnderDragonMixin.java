package io.izzel.arclight.common.mixin.core.world.entity.boss.enderdragon;

import io.izzel.arclight.common.mixin.core.world.entity.MobMixin;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.enderdragon.phases.DragonPhaseInstance;
import net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhase;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v.block.CraftBlock;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.ArrayList;
import java.util.List;

@Mixin(EnderDragon.class)
public abstract class EnderDragonMixin extends MobMixin {

    private Explosion explosionSource = new Explosion(null, (EnderDragon) (Object) this, null, null, Double.NaN, Double.NaN, Double.NaN, Float.NaN, true, Explosion.BlockInteraction.DESTROY);

    @Redirect(method = "aiStep", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/boss/enderdragon/phases/DragonPhaseInstance;getFlyTargetLocation()Lnet/minecraft/world/phys/Vec3;"))
    private Vec3 arclight$noMoveHovering(DragonPhaseInstance phase) {
        Vec3 vec3d = phase.getFlyTargetLocation();
        return vec3d != null && phase.getPhase() != EnderDragonPhase.HOVERING ? vec3d : null;
    }

    @Redirect(method = "checkCrystals", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/boss/enderdragon/EnderDragon;setHealth(F)V"))
    private void arclight$regainHealth(EnderDragon enderDragonEntity, float health) {
        EntityRegainHealthEvent event = new EntityRegainHealthEvent(this.getBukkitEntity(), 1.0F, EntityRegainHealthEvent.RegainReason.ENDER_CRYSTAL);
        Bukkit.getPluginManager().callEvent(event);

        if (!event.isCancelled()) {
            this.setHealth((float) (this.getHealth() + event.getAmount()));
        }
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    private boolean checkWalls(final AABB axisalignedbb) {
        final int i = Mth.floor(axisalignedbb.minX);
        final int j = Mth.floor(axisalignedbb.minY);
        final int k = Mth.floor(axisalignedbb.minZ);
        final int l = Mth.floor(axisalignedbb.maxX);
        final int i2 = Mth.floor(axisalignedbb.maxY);
        final int j2 = Mth.floor(axisalignedbb.maxZ);
        boolean flag = false;
        boolean flag2 = false;
        final List<org.bukkit.block.Block> destroyedBlocks = new ArrayList<>();
        for (int k2 = i; k2 <= l; ++k2) {
            for (int l2 = j; l2 <= i2; ++l2) {
                for (int i3 = k; i3 <= j2; ++i3) {
                    final BlockPos blockposition = new BlockPos(k2, l2, i3);
                    final BlockState iblockdata = this.level.getBlockState(blockposition);
                    final net.minecraft.world.level.block.Block block = iblockdata.getBlock();
                    if (!iblockdata.isAir() && iblockdata.getMaterial() != Material.FIRE) {
                        if (net.minecraftforge.common.ForgeHooks.canEntityDestroy(this.level, blockposition, (EnderDragon) (Object) this) && !BlockTags.DRAGON_IMMUNE.contains(block)) {
                            flag2 = true;
                            destroyedBlocks.add(CraftBlock.at(this.level, blockposition));
                        } else {
                            flag = true;
                        }
                    }
                }
            }
        }
        if (!flag2) {
            return flag;
        }
        final org.bukkit.entity.Entity bukkitEntity = this.getBukkitEntity();
        final EntityExplodeEvent event = new EntityExplodeEvent(bukkitEntity, bukkitEntity.getLocation(), destroyedBlocks, 0.0f);
        bukkitEntity.getServer().getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return flag;
        }
        if (event.getYield() == 0.0f) {
            for (final org.bukkit.block.Block block2 : event.blockList()) {
                this.level.removeBlock(new BlockPos(block2.getX(), block2.getY(), block2.getZ()), false);
            }
        } else {
            for (final org.bukkit.block.Block block2 : event.blockList()) {
                final org.bukkit.Material blockId = block2.getType();
                if (blockId.isAir()) {
                    continue;
                }
                final CraftBlock craftBlock = (CraftBlock) block2;
                final BlockPos blockposition2 = craftBlock.getPosition();
                final net.minecraft.world.level.block.Block nmsBlock = craftBlock.getNMS().getBlock();
                if (nmsBlock.dropFromExplosion(this.explosionSource)) {
                    BlockEntity tileentity = craftBlock.getNMS().hasBlockEntity() ? this.level.getBlockEntity(blockposition2) : null;
                    LootContext.Builder loottableinfo_builder = new LootContext.Builder((ServerLevel)this.level).withRandom(this.level.random).withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(blockposition2)).withParameter(LootContextParams.TOOL, ItemStack.EMPTY).withParameter(LootContextParams.EXPLOSION_RADIUS, 1.0f / event.getYield()).withOptionalParameter(LootContextParams.BLOCK_ENTITY, tileentity);
                    for (ItemStack stack : craftBlock.getNMS().getDrops(loottableinfo_builder)) {
                        Block.popResource(this.level, blockposition2, stack);
                    }
                    craftBlock.getNMS().spawnAfterBreak((ServerLevel) this.level, blockposition2, ItemStack.EMPTY);
                    // net.minecraft.block.Block.spawnDrops(craftBlock.getNMS(), loottableinfo_builder);
                }
                nmsBlock.wasExploded(this.level, blockposition2, this.explosionSource);
                this.level.removeBlock(blockposition2, false);
            }
        }
        if (flag2) {
            final BlockPos blockposition3 = new BlockPos(i + this.random.nextInt(l - i + 1), j + this.random.nextInt(i2 - j + 1), k + this.random.nextInt(j2 - k + 1));
            this.level.levelEvent(2008, blockposition3, 0);
        }
        return flag;
    }

}
