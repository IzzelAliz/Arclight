package io.izzel.arclight.common.mixin.core.block;

import io.izzel.arclight.common.bridge.entity.EntityBridge;
import net.minecraft.block.BlockState;
import net.minecraft.block.CauldronBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.potion.PotionUtils;
import net.minecraft.potion.Potions;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v.block.CraftBlock;
import org.bukkit.event.block.CauldronLevelChangeEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CauldronBlock.class)
public class CauldronBlockMixin {

    @Inject(method = "onEntityCollision", cancellable = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;extinguish()V"))
    public void arclight$extinguish(BlockState state, World worldIn, BlockPos pos, Entity entityIn, CallbackInfo ci) {
        int i = state.get(CauldronBlock.LEVEL);
        if (!changeLevel(worldIn, pos, state, i - 1, entityIn, CauldronLevelChangeEvent.ChangeReason.EXTINGUISH)) {
            ci.cancel();
        }
    }

    @Inject(method = "onBlockActivated", cancellable = true, at = @At(value = "FIELD", target = "Lnet/minecraft/entity/player/PlayerEntity;abilities:Lnet/minecraft/entity/player/PlayerAbilities;"))
    public void arclight$levelChange(BlockState state, World worldIn, BlockPos pos, PlayerEntity player, Hand handIn, BlockRayTraceResult hit, CallbackInfoReturnable<ActionResultType> cir) {
        ItemStack itemStack = player.getHeldItem(handIn);
        Item item = itemStack.getItem();
        int i = state.get(CauldronBlock.LEVEL);
        int newLevel;
        CauldronLevelChangeEvent.ChangeReason reason;
        if (item == Items.WATER_BUCKET) {
            reason = CauldronLevelChangeEvent.ChangeReason.BUCKET_EMPTY;
            newLevel = 3;
        } else if (item == Items.BUCKET) {
            reason = CauldronLevelChangeEvent.ChangeReason.BUCKET_FILL;
            newLevel = 0;
        } else if (item == Items.GLASS_BOTTLE) {
            reason = CauldronLevelChangeEvent.ChangeReason.BOTTLE_FILL;
            newLevel = i - 1;
        } else if (item == Items.POTION && PotionUtils.getPotionFromItem(itemStack) == Potions.WATER) {
            reason = CauldronLevelChangeEvent.ChangeReason.BOTTLE_EMPTY;
            newLevel = i + 1;
        } else {
            reason = null;
            newLevel = 0;
        }
        if (reason != null && !changeLevel(worldIn, pos, state, newLevel, player, reason)) {
            cir.setReturnValue(ActionResultType.SUCCESS);
        }
    }

    @Inject(method = "onBlockActivated", cancellable = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/item/IDyeableArmorItem;removeColor(Lnet/minecraft/item/ItemStack;)V"))
    public void arclight$removeColor(BlockState state, World worldIn, BlockPos pos, PlayerEntity player, Hand handIn, BlockRayTraceResult hit, CallbackInfoReturnable<ActionResultType> cir) {
        int i = state.get(CauldronBlock.LEVEL);
        if (!changeLevel(worldIn, pos, state, i - 1, player, CauldronLevelChangeEvent.ChangeReason.ARMOR_WASH)) {
            cir.setReturnValue(ActionResultType.SUCCESS);
        }
    }

    @Inject(method = "onBlockActivated", cancellable = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/tileentity/BannerTileEntity;removeBannerData(Lnet/minecraft/item/ItemStack;)V"))
    public void arclight$removeBanner(BlockState state, World worldIn, BlockPos pos, PlayerEntity player, Hand handIn, BlockRayTraceResult hit, CallbackInfoReturnable<ActionResultType> cir) {
        int i = state.get(CauldronBlock.LEVEL);
        if (!changeLevel(worldIn, pos, state, i - 1, player, CauldronLevelChangeEvent.ChangeReason.BANNER_WASH)) {
            cir.setReturnValue(ActionResultType.SUCCESS);
        }
    }

    @Inject(method = "fillWithRain", cancellable = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;I)Z"))
    public void arclight$fillRain(World worldIn, BlockPos pos, CallbackInfo ci) {
        BlockState state = worldIn.getBlockState(pos);
        BlockState cycle = state.func_235896_a_(CauldronBlock.LEVEL);
        int newLevel = cycle.get(CauldronBlock.LEVEL);
        if (!changeLevel(worldIn, pos, state, newLevel, null, CauldronLevelChangeEvent.ChangeReason.UNKNOWN)) {
            ci.cancel();
        }
    }

    private boolean changeLevel(World world, BlockPos pos, BlockState state, int i, Entity entity, CauldronLevelChangeEvent.ChangeReason reason) {
        int newLevel = MathHelper.clamp(i, 0, 3);
        CauldronLevelChangeEvent event = new CauldronLevelChangeEvent(
            CraftBlock.at(world, pos),
            (entity == null) ? null : ((EntityBridge) entity).bridge$getBukkitEntity(),
            reason, state.get(CauldronBlock.LEVEL), newLevel
        );
        Bukkit.getPluginManager().callEvent(event);
        return !event.isCancelled();
    }
}
