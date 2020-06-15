package io.izzel.arclight.common.mixin.v1_15.server.management;

import io.izzel.arclight.common.bridge.entity.player.ServerPlayerEntityBridge;
import io.izzel.arclight.common.bridge.server.management.PlayerInteractionManagerBridge;
import net.minecraft.block.BlockState;
import net.minecraft.block.CakeBlock;
import net.minecraft.block.DoorBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.container.INamedContainerProvider;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUseContext;
import net.minecraft.network.play.client.CPlayerDiggingPacket;
import net.minecraft.network.play.server.SChangeBlockPacket;
import net.minecraft.network.play.server.SPlayerDiggingPacket;
import net.minecraft.server.management.PlayerInteractionManager;
import net.minecraft.state.properties.DoubleBlockHalf;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.world.GameType;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.ForgeHooks;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.bukkit.event.Event;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(PlayerInteractionManager.class)
public abstract class PlayerInteractionManagerMixin_1_15 implements PlayerInteractionManagerBridge {

    // @formatter:off
    @Shadow public abstract void func_229860_a_(BlockPos p_229860_1_, CPlayerDiggingPacket.Action p_229860_2_, String p_229860_3_);
    @Shadow private GameType gameType;
    @Shadow public abstract boolean isCreative();
    @Shadow public ServerWorld world;
    // @formatter:on

    @Override
    public SPlayerDiggingPacket bridge$diggingPacket(BlockPos pos, BlockState state, CPlayerDiggingPacket.Action action, boolean successful, String context) {
        return new SPlayerDiggingPacket(pos, state, action, successful, context);
    }

    @Override
    public void bridge$creativeHarvestBlock(BlockPos pos, CPlayerDiggingPacket.Action action, String context) {
        this.func_229860_a_(pos, action, context);
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public ActionResultType func_219441_a(PlayerEntity playerIn, World worldIn, ItemStack stackIn, Hand handIn, BlockRayTraceResult blockRaytraceResultIn) {
        BlockPos blockpos = blockRaytraceResultIn.getPos();
        BlockState blockstate = worldIn.getBlockState(blockpos);
        ActionResultType resultType = ActionResultType.PASS;
        boolean cancelledBlock = false;
        if (this.gameType == GameType.SPECTATOR) {
            INamedContainerProvider provider = blockstate.getContainer(worldIn, blockpos);
            cancelledBlock = !(provider instanceof INamedContainerProvider);
        }
        if (playerIn.getCooldownTracker().hasCooldown(stackIn.getItem())) {
            cancelledBlock = true;
        }

        PlayerInteractEvent bukkitEvent = CraftEventFactory.callPlayerInteractEvent(playerIn, Action.RIGHT_CLICK_BLOCK, blockpos, blockRaytraceResultIn.getFace(), stackIn, cancelledBlock, handIn);
        bridge$setFiredInteract(true);
        bridge$setInteractResult(bukkitEvent.useItemInHand() == Event.Result.DENY);
        if (bukkitEvent.useInteractedBlock() == Event.Result.DENY) {
            if (blockstate.getBlock() instanceof DoorBlock) {
                boolean bottom = blockstate.get(DoorBlock.HALF) == DoubleBlockHalf.LOWER;
                ((ServerPlayerEntity) playerIn).connection.sendPacket(new SChangeBlockPacket(this.world, bottom ? blockpos.up() : blockpos.down()));
            } else if (blockstate.getBlock() instanceof CakeBlock) {
                ((ServerPlayerEntityBridge) playerIn).bridge$getBukkitEntity().sendHealthUpdate();
            }
            ((ServerPlayerEntityBridge) playerIn).bridge$getBukkitEntity().updateInventory();
            resultType = ((bukkitEvent.useItemInHand() != Event.Result.ALLOW) ? ActionResultType.SUCCESS : ActionResultType.PASS);
        } else if (this.gameType == GameType.SPECTATOR) {
            INamedContainerProvider inamedcontainerprovider = blockstate.getContainer(worldIn, blockpos);
            if (inamedcontainerprovider != null) {
                playerIn.openContainer(inamedcontainerprovider);
                return ActionResultType.SUCCESS;
            } else {
                return ActionResultType.PASS;
            }
        } else {
            net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickBlock event = ForgeHooks.onRightClickBlock(playerIn, handIn, blockpos, blockRaytraceResultIn.getFace());
            if (event.isCanceled()) return event.getCancellationResult();
            ItemUseContext itemusecontext = new ItemUseContext(playerIn, handIn, blockRaytraceResultIn);
            if (event.getUseItem() != net.minecraftforge.eventbus.api.Event.Result.DENY) {
                ActionResultType result = stackIn.onItemUseFirst(itemusecontext);
                if (result != ActionResultType.PASS) return result;
            }
            boolean flag = !playerIn.getHeldItemMainhand().isEmpty() || !playerIn.getHeldItemOffhand().isEmpty();
            boolean flag1 = (playerIn.isSecondaryUseActive() && flag) && !(playerIn.getHeldItemMainhand().doesSneakBypassUse(worldIn, blockpos, playerIn) && playerIn.getHeldItemOffhand().doesSneakBypassUse(worldIn, blockpos, playerIn));
            if (event.getUseBlock() != net.minecraftforge.eventbus.api.Event.Result.DENY && !flag1) {
                resultType = blockstate.onBlockActivated(worldIn, playerIn, handIn, blockRaytraceResultIn);
                if (resultType.isSuccessOrConsume()) {
                    return resultType;
                }
            }
            if (!stackIn.isEmpty() && resultType != ActionResultType.SUCCESS && !bridge$getInteractResult()) {
                if (event.getUseItem() == net.minecraftforge.eventbus.api.Event.Result.DENY) {
                    return ActionResultType.PASS;
                }
                if (this.isCreative()) {
                    int i = stackIn.getCount();
                    resultType = stackIn.onItemUse(itemusecontext);
                    stackIn.setCount(i);
                    return resultType;
                } else {
                    return stackIn.onItemUse(itemusecontext);
                }
            } else {
                return resultType;
            }
        }
        return resultType;
    }
}
