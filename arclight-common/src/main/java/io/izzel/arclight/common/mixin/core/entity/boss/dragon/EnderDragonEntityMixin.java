package io.izzel.arclight.common.mixin.core.entity.boss.dragon;

import io.izzel.arclight.common.mixin.core.entity.MobEntityMixin;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.material.Material;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.boss.dragon.phase.IPhase;
import net.minecraft.entity.boss.dragon.phase.PhaseType;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.LootContext;
import net.minecraft.loot.LootParameters;
import net.minecraft.tags.BlockTags;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.Explosion;
import net.minecraft.world.server.ServerWorld;
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

@Mixin(EnderDragonEntity.class)
public abstract class EnderDragonEntityMixin extends MobEntityMixin {

    private Explosion explosionSource = new Explosion(null, (EnderDragonEntity) (Object) this, null, null, Double.NaN, Double.NaN, Double.NaN, Float.NaN, true, Explosion.Mode.DESTROY);

    @Redirect(method = "livingTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/boss/dragon/phase/IPhase;getTargetLocation()Lnet/minecraft/util/math/vector/Vector3d;"))
    private Vector3d arclight$noMoveHovering(IPhase phase) {
        Vector3d vec3d = phase.getTargetLocation();
        return vec3d != null && phase.getType() != PhaseType.HOVER ? vec3d : null;
    }

    @Redirect(method = "updateDragonEnderCrystal", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/boss/dragon/EnderDragonEntity;setHealth(F)V"))
    private void arclight$regainHealth(EnderDragonEntity enderDragonEntity, float health) {
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
    private boolean destroyBlocksInAABB(final AxisAlignedBB axisalignedbb) {
        final int i = MathHelper.floor(axisalignedbb.minX);
        final int j = MathHelper.floor(axisalignedbb.minY);
        final int k = MathHelper.floor(axisalignedbb.minZ);
        final int l = MathHelper.floor(axisalignedbb.maxX);
        final int i2 = MathHelper.floor(axisalignedbb.maxY);
        final int j2 = MathHelper.floor(axisalignedbb.maxZ);
        boolean flag = false;
        boolean flag2 = false;
        final List<org.bukkit.block.Block> destroyedBlocks = new ArrayList<>();
        for (int k2 = i; k2 <= l; ++k2) {
            for (int l2 = j; l2 <= i2; ++l2) {
                for (int i3 = k; i3 <= j2; ++i3) {
                    final BlockPos blockposition = new BlockPos(k2, l2, i3);
                    final BlockState iblockdata = this.world.getBlockState(blockposition);
                    final net.minecraft.block.Block block = iblockdata.getBlock();
                    if (!iblockdata.isAir() && iblockdata.getMaterial() != Material.FIRE) {
                        if (net.minecraftforge.common.ForgeHooks.canEntityDestroy(this.world, blockposition, (EnderDragonEntity) (Object) this) && !BlockTags.DRAGON_IMMUNE.contains(block)) {
                            flag2 = true;
                            destroyedBlocks.add(CraftBlock.at(this.world, blockposition));
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
                this.world.removeBlock(new BlockPos(block2.getX(), block2.getY(), block2.getZ()), false);
            }
        } else {
            for (final org.bukkit.block.Block block2 : event.blockList()) {
                final org.bukkit.Material blockId = block2.getType();
                if (blockId.isAir()) {
                    continue;
                }
                final CraftBlock craftBlock = (CraftBlock) block2;
                final BlockPos blockposition2 = craftBlock.getPosition();
                final net.minecraft.block.Block nmsBlock = craftBlock.getNMS().getBlock();
                if (nmsBlock.canDropFromExplosion(this.explosionSource)) {
                    TileEntity tileentity = nmsBlock.hasTileEntity(craftBlock.getNMS()) ? this.world.getTileEntity(blockposition2) : null;
                    LootContext.Builder loottableinfo_builder = new LootContext.Builder((ServerWorld)this.world).withRandom(this.world.rand).withParameter(LootParameters.field_237457_g_, Vector3d.copyCentered(blockposition2)).withParameter(LootParameters.TOOL, ItemStack.EMPTY).withParameter(LootParameters.EXPLOSION_RADIUS, 1.0f / event.getYield()).withNullableParameter(LootParameters.BLOCK_ENTITY, tileentity);
                    for (ItemStack stack : craftBlock.getNMS().getDrops(loottableinfo_builder)) {
                        Block.spawnAsEntity(this.world, blockposition2, stack);
                    }
                    craftBlock.getNMS().spawnAdditionalDrops((ServerWorld) this.world, blockposition2, ItemStack.EMPTY);
                    // net.minecraft.block.Block.spawnDrops(craftBlock.getNMS(), loottableinfo_builder);
                }
                nmsBlock.onExplosionDestroy(this.world, blockposition2, this.explosionSource);
                this.world.removeBlock(blockposition2, false);
            }
        }
        if (flag2) {
            final BlockPos blockposition3 = new BlockPos(i + this.rand.nextInt(l - i + 1), j + this.rand.nextInt(i2 - j + 1), k + this.rand.nextInt(j2 - k + 1));
            this.world.playEvent(2008, blockposition3, 0);
        }
        return flag;
    }

}
