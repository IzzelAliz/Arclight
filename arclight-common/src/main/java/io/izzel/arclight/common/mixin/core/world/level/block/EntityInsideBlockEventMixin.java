package io.izzel.arclight.common.mixin.core.world.level.block;

import io.izzel.arclight.common.bridge.core.entity.EntityBridge;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import io.papermc.paper.event.entity.EntityInsideBlockEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.BasePressurePlateBlock;
import net.minecraft.world.level.block.BigDripleafBlock;
import net.minecraft.world.level.block.BubbleColumnBlock;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.CactusBlock;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.DetectorRailBlock;
import net.minecraft.world.level.block.EndGatewayBlock;
import net.minecraft.world.level.block.EndPortalBlock;
import net.minecraft.world.level.block.EyeblossomBlock;
import net.minecraft.world.level.block.FrogspawnBlock;
import net.minecraft.world.level.block.HoneyBlock;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.LavaCauldronBlock;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.LilyPadBlock;
import net.minecraft.world.level.block.NetherPortalBlock;
import net.minecraft.world.level.block.PitcherCropBlock;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.WebBlock;
import net.minecraft.world.level.block.WitherRoseBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.craftbukkit.block.CraftBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

// Every target declares this exact overload on both native inputs. In particular,
// PitcherCropBlock's independent implementation cannot be covered by hooking CropBlock alone.
// Powder snow and tripwire have their own cancellation/pre-dispatch contracts.
@Mixin({
    BaseFireBlock.class,
    BasePressurePlateBlock.class,
    BigDripleafBlock.class,
    BubbleColumnBlock.class,
    ButtonBlock.class,
    CactusBlock.class,
    CampfireBlock.class,
    CropBlock.class,
    DetectorRailBlock.class,
    EndGatewayBlock.class,
    EndPortalBlock.class,
    EyeblossomBlock.class,
    FrogspawnBlock.class,
    HoneyBlock.class,
    HopperBlock.class,
    LavaCauldronBlock.class,
    LayeredCauldronBlock.class,
    LilyPadBlock.class,
    NetherPortalBlock.class,
    PitcherCropBlock.class,
    SweetBerryBushBlock.class,
    WebBlock.class,
    WitherRoseBlock.class
})
public abstract class EntityInsideBlockEventMixin {

    @Decorate(method = "entityInside(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/entity/InsideBlockEffectApplier;Z)V",
        inject = true, at = @At("HEAD"), require = 1, allow = 1)
    private void arclight$entityInsideBlock(BlockState state, Level level, BlockPos pos, Entity entity,
                                          InsideBlockEffectApplier effectApplier, boolean isPrecise) throws Throwable {
        if (!level.isClientSide()
            && !new EntityInsideBlockEvent(((EntityBridge) entity).bridge$getBukkitEntity(), CraftBlock.at(level, pos)).callEvent()) {
            // Stop this owner's complete native body, before damage, movement,
            // portal entry, block changes or deferred effects are scheduled.
            DecorationOps.cancel().invoke();
            return;
        }
    }
}
