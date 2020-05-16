package io.izzel.arclight.mixin.core.server.management;

import io.izzel.arclight.bridge.entity.player.ServerPlayerEntityBridge;
import io.izzel.arclight.bridge.server.management.PlayerInteractionManagerBridge;
import net.minecraft.block.BlockState;
import net.minecraft.block.CakeBlock;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.TrapDoorBlock;
import net.minecraft.entity.item.ItemEntity;
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
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.world.GameType;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.ForgeHooks;
import org.bukkit.craftbukkit.v1_14_R1.block.CraftBlock;
import org.bukkit.craftbukkit.v1_14_R1.event.CraftEventFactory;
import org.bukkit.event.Event;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import io.izzel.arclight.mod.util.ArclightCaptures;

import java.util.List;

@Mixin(PlayerInteractionManager.class)
public abstract class PlayerInteractionManagerMixin implements PlayerInteractionManagerBridge {

    // @formatter:off
    @Shadow public ServerWorld world;
    @Shadow public ServerPlayerEntity player;
    @Shadow public abstract boolean isCreative();
    @Shadow public abstract void func_225415_a(BlockPos p_225415_1_, CPlayerDiggingPacket.Action p_225415_2_);
    @Shadow private GameType gameType;
    @Shadow private int initialDamage;
    @Shadow private int ticks;
    @Shadow private boolean isDestroyingBlock;
    @Shadow private BlockPos destroyPos;
    @Shadow private int durabilityRemainingOnBlock;
    @Shadow private boolean receivedFinishDiggingPacket;
    @Shadow private BlockPos delayedDestroyPos;
    @Shadow private int initialBlockDamage;
    // @formatter:on

    public boolean interactResult = false;
    public boolean firedInteract = false;

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public void func_225416_a(BlockPos blockPos, CPlayerDiggingPacket.Action action, Direction direction, int i) {
        double d0 = this.player.posX - (blockPos.getX() + 0.5);
        double d2 = this.player.posY - (blockPos.getY() + 0.5) + 1.5;
        double d3 = this.player.posZ - (blockPos.getZ() + 0.5);
        double d4 = d0 * d0 + d2 * d2 + d3 * d3;
        double dist = player.getAttribute(net.minecraft.entity.player.PlayerEntity.REACH_DISTANCE).getValue() + 1;
        dist *= dist;
        net.minecraftforge.event.entity.player.PlayerInteractEvent.LeftClickBlock forgeEvent = net.minecraftforge.common.ForgeHooks.onLeftClickBlock(player, blockPos, direction);
        if (forgeEvent.isCanceled() || (!this.isCreative() && forgeEvent.getUseItem() == net.minecraftforge.eventbus.api.Event.Result.DENY)) { // Restore block and te data
            player.connection.sendPacket(new SPlayerDiggingPacket(blockPos, world.getBlockState(blockPos), action, false));
            world.notifyBlockUpdate(blockPos, world.getBlockState(blockPos), world.getBlockState(blockPos), 3);
            return;
        }
        if (d4 > dist) {
            this.player.connection.sendPacket(new SPlayerDiggingPacket(blockPos, this.world.getBlockState(blockPos), action, false));
        } else if (blockPos.getY() >= i) {
            this.player.connection.sendPacket(new SPlayerDiggingPacket(blockPos, this.world.getBlockState(blockPos), action, false));
        } else if (action == CPlayerDiggingPacket.Action.START_DESTROY_BLOCK) {
            if (!this.world.isBlockModifiable(this.player, blockPos)) {
                CraftEventFactory.callPlayerInteractEvent(this.player, Action.LEFT_CLICK_BLOCK, blockPos, direction, this.player.inventory.getCurrentItem(), Hand.MAIN_HAND);
                this.player.connection.sendPacket(new SPlayerDiggingPacket(blockPos, this.world.getBlockState(blockPos), action, false));
                TileEntity tileentity = this.world.getTileEntity(blockPos);
                if (tileentity != null) {
                    this.player.connection.sendPacket(tileentity.getUpdatePacket());
                }
                return;
            }
            PlayerInteractEvent event = CraftEventFactory.callPlayerInteractEvent(this.player, Action.LEFT_CLICK_BLOCK, blockPos, direction, this.player.inventory.getCurrentItem(), Hand.MAIN_HAND);
            if (event.isCancelled()) {
                this.player.connection.sendPacket(new SChangeBlockPacket(this.world, blockPos));
                TileEntity tileentity2 = this.world.getTileEntity(blockPos);
                if (tileentity2 != null) {
                    this.player.connection.sendPacket(tileentity2.getUpdatePacket());
                }
                return;
            }
            if (this.isCreative()) {
                if (!this.world.extinguishFire(null, blockPos, direction)) {
                    this.func_225415_a(blockPos, action);
                } else {
                    this.player.connection.sendPacket(new SPlayerDiggingPacket(blockPos, this.world.getBlockState(blockPos), action, true));
                }
                return;
            }
            if (this.player.func_223729_a(this.world, blockPos, this.gameType)) {
                this.player.connection.sendPacket(new SPlayerDiggingPacket(blockPos, this.world.getBlockState(blockPos), action, false));
                return;
            }
            this.initialDamage = this.ticks;
            float f = 1.0f;
            BlockState iblockdata = this.world.getBlockState(blockPos);
            if (event.useInteractedBlock() == org.bukkit.event.Event.Result.DENY) {
                BlockState data = this.world.getBlockState(blockPos);
                if (data.getBlock() instanceof DoorBlock) {
                    boolean bottom = data.get(DoorBlock.HALF) == DoubleBlockHalf.LOWER;
                    this.player.connection.sendPacket(new SChangeBlockPacket(this.world, blockPos));
                    this.player.connection.sendPacket(new SChangeBlockPacket(this.world, bottom ? blockPos.up() : blockPos.down()));
                } else if (data.getBlock() instanceof TrapDoorBlock) {
                    this.player.connection.sendPacket(new SChangeBlockPacket(this.world, blockPos));
                }
            } else if (!iblockdata.isAir()) {
                if (forgeEvent.getUseBlock() != net.minecraftforge.eventbus.api.Event.Result.DENY) {
                    iblockdata.onBlockClicked(this.world, blockPos, this.player);
                }
                f = iblockdata.getPlayerRelativeBlockHardness(this.player, this.player.world, blockPos);
                this.world.extinguishFire(null, blockPos, direction);
            }
            if (event.useItemInHand() == Event.Result.DENY) {
                if (f > 1.0f) {
                    this.player.connection.sendPacket(new SChangeBlockPacket(this.world, blockPos));
                }
                return;
            }
            BlockDamageEvent blockEvent = CraftEventFactory.callBlockDamageEvent(this.player, blockPos.getX(), blockPos.getY(), blockPos.getZ(), this.player.inventory.getCurrentItem(), f >= 1.0f);
            if (blockEvent.isCancelled()) {
                this.player.connection.sendPacket(new SChangeBlockPacket(this.world, blockPos));
                return;
            }
            if (blockEvent.getInstaBreak()) {
                f = 2.0f;
            }
            if (!iblockdata.isAir() && f >= 1.0f) {
                this.func_225415_a(blockPos, action);
            } else {
                this.isDestroyingBlock = true;
                this.destroyPos = blockPos;
                int j = (int) (f * 10.0f);
                this.world.sendBlockBreakProgress(this.player.getEntityId(), blockPos, j);
                this.player.connection.sendPacket(new SPlayerDiggingPacket(blockPos, this.world.getBlockState(blockPos), action, true));
                this.durabilityRemainingOnBlock = j;
            }
        } else if (action == CPlayerDiggingPacket.Action.STOP_DESTROY_BLOCK) {
            if (blockPos.equals(this.destroyPos)) {
                int k = this.ticks - this.initialDamage;
                BlockState iblockdata = this.world.getBlockState(blockPos);
                if (!iblockdata.isAir()) {
                    float f2 = iblockdata.getPlayerRelativeBlockHardness(this.player, this.player.world, blockPos) * (k + 1);
                    if (f2 >= 0.7f) {
                        this.isDestroyingBlock = false;
                        this.world.sendBlockBreakProgress(this.player.getEntityId(), blockPos, -1);
                        this.func_225415_a(blockPos, action);
                        return;
                    }
                    if (!this.receivedFinishDiggingPacket) {
                        this.isDestroyingBlock = false;
                        this.receivedFinishDiggingPacket = true;
                        this.delayedDestroyPos = blockPos;
                        this.initialBlockDamage = this.initialDamage;
                    }
                }
            }
            this.player.connection.sendPacket(new SPlayerDiggingPacket(blockPos, this.world.getBlockState(blockPos), action, true));
        } else if (action == CPlayerDiggingPacket.Action.ABORT_DESTROY_BLOCK) {
            this.isDestroyingBlock = false;
            this.world.sendBlockBreakProgress(this.player.getEntityId(), this.destroyPos, -1);
            this.player.connection.sendPacket(new SPlayerDiggingPacket(blockPos, this.world.getBlockState(blockPos), action, true));
        }
    }

    @Inject(method = "tryHarvestBlock", at = @At("RETURN"))
    public void arclight$resetBlockBreak(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        List<ItemEntity> blockDrops = ArclightCaptures.getBlockDrops();
        org.bukkit.block.BlockState state = ArclightCaptures.getBlockBreakPlayerState();
        BlockBreakEvent breakEvent = ArclightCaptures.resetBlockBreakPlayer();
        if (blockDrops != null && (breakEvent == null || breakEvent.isDropItems())) {
            CraftBlock craftBlock = CraftBlock.at(this.world, pos);
            CraftEventFactory.handleBlockDropItemEvent(craftBlock, state, this.player, blockDrops);
        }
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
        firedInteract = true;
        interactResult = bukkitEvent.useItemInHand() == Event.Result.DENY;
        if (bukkitEvent.useInteractedBlock() == Event.Result.DENY) {
            if (blockstate.getBlock() instanceof DoorBlock) {
                boolean bottom = blockstate.get(DoorBlock.HALF) == DoubleBlockHalf.LOWER;
                ((ServerPlayerEntity) playerIn).connection.sendPacket(new SChangeBlockPacket(world, bottom ? blockpos.up() : blockpos.down()));
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
            boolean flag1 = !(playerIn.isSneaking() && flag) || (playerIn.getHeldItemMainhand().doesSneakBypassUse(worldIn, blockpos, playerIn) && playerIn.getHeldItemOffhand().doesSneakBypassUse(worldIn, blockpos, playerIn));
            boolean flag2 =  blockstate.onBlockActivated(worldIn, playerIn, handIn, blockRaytraceResultIn);
            if (event.getUseBlock() != net.minecraftforge.eventbus.api.Event.Result.DENY && flag1 && flag2) {
                return ActionResultType.SUCCESS;
            } else {
                if (flag1) {
                    resultType = flag2 ? ActionResultType.SUCCESS : ActionResultType.FAIL;
                }
                if (!stackIn.isEmpty() && resultType != ActionResultType.SUCCESS && !interactResult) {
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
        }
        return resultType;
    }

    @Override
    public boolean bridge$isFiredInteract() {
        return firedInteract;
    }

    @Override
    public void bridge$setFiredInteract(boolean b) {
        this.firedInteract = b;
    }

    @Override
    public boolean bridge$getInteractResult() {
        return interactResult;
    }

    @Override
    public void bridge$setInteractResult(boolean b) {
        this.interactResult = b;
    }
}
