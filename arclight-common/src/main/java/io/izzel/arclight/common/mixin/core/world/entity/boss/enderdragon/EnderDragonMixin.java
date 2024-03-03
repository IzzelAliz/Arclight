package io.izzel.arclight.common.mixin.core.world.entity.boss.enderdragon;

import io.izzel.arclight.common.mixin.core.world.entity.MobMixin;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.enderdragon.phases.DragonPhaseInstance;
import net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhase;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.dimension.end.EndDragonFight;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v.block.CraftBlock;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.EntityRemoveEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

@Mixin(EnderDragon.class)
public abstract class EnderDragonMixin extends MobMixin {

    @Shadow @Nullable private EndDragonFight dragonFight;

    private final Explosion explosionSource = new Explosion(this.level(), (EnderDragon) (Object) this, null, null, Double.NaN, Double.NaN, Double.NaN, Float.NaN, true, Explosion.BlockInteraction.DESTROY, ParticleTypes.EXPLOSION, ParticleTypes.EXPLOSION_EMITTER, SoundEvents.GENERIC_EXPLODE);

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

    @Inject(method = "kill", at = @At("HEAD"))
    private void arclight$killed(CallbackInfo ci) {
        this.bridge$pushEntityRemoveCause(EntityRemoveEvent.Cause.DEATH);
    }

    @Inject(method = "tickDeath", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/boss/enderdragon/EnderDragon;remove(Lnet/minecraft/world/entity/Entity$RemovalReason;)V"))
    private void arclight$killed2(CallbackInfo ci) {
        this.bridge$pushEntityRemoveCause(EntityRemoveEvent.Cause.DEATH);
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
                    final BlockState iblockdata = this.level().getBlockState(blockposition);
                    if (!iblockdata.isAir() && !iblockdata.is(BlockTags.DRAGON_TRANSPARENT)) {
                        if (this.bridge$forge$canEntityDestroy(this.level(), blockposition, (EnderDragon) (Object) this) && !iblockdata.is(BlockTags.DRAGON_IMMUNE)) {
                            flag2 = true;
                            destroyedBlocks.add(CraftBlock.at(this.level(), blockposition));
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
                this.level().removeBlock(new BlockPos(block2.getX(), block2.getY(), block2.getZ()), false);
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
                    BlockEntity tileentity = craftBlock.getNMS().hasBlockEntity() ? this.level().getBlockEntity(blockposition2) : null;
                    LootParams.Builder loottableinfo_builder = new LootParams.Builder((ServerLevel) this.level()).withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(blockposition2)).withParameter(LootContextParams.TOOL, ItemStack.EMPTY).withParameter(LootContextParams.EXPLOSION_RADIUS, 1.0f / event.getYield()).withOptionalParameter(LootContextParams.BLOCK_ENTITY, tileentity);
                    for (ItemStack stack : craftBlock.getNMS().getDrops(loottableinfo_builder)) {
                        Block.popResource(this.level(), blockposition2, stack);
                    }
                    craftBlock.getNMS().spawnAfterBreak((ServerLevel) this.level(), blockposition2, ItemStack.EMPTY, false);
                    // net.minecraft.block.Block.spawnDrops(craftBlock.getNMS(), loottableinfo_builder);
                }
                nmsBlock.wasExploded(this.level(), blockposition2, this.explosionSource);
                this.level().removeBlock(blockposition2, false);
            }
        }
        if (flag2) {
            final BlockPos blockposition3 = new BlockPos(i + this.random.nextInt(l - i + 1), j + this.random.nextInt(i2 - j + 1), k + this.random.nextInt(j2 - k + 1));
            this.level().levelEvent(2008, blockposition3, 0);
        }
        return flag;
    }

    // TODO FIXME: exp patch for end dragon
    @Override
    public int getExpReward() {
        // CraftBukkit - Moved from #tickDeath method
        boolean flag = this.level().getGameRules().getBoolean(GameRules.RULE_DOMOBLOOT);
        short short0 = 500;

        if (this.dragonFight != null && !this.dragonFight.hasPreviouslyKilledDragon()) {
            short0 = 12000;
        }

        return flag ? short0 : 0;
    }
}
