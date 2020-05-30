package io.izzel.arclight.common.mixin.core.block;

import io.izzel.arclight.common.bridge.block.NetherPortalBlockBridge;
import io.izzel.arclight.common.bridge.world.WorldBridge;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.NetherPortalBlock;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IWorld;
import org.bukkit.Bukkit;
import org.bukkit.block.BlockState;
import org.bukkit.craftbukkit.v.CraftWorld;
import org.bukkit.craftbukkit.v.block.CraftBlock;
import org.bukkit.craftbukkit.v.block.CraftBlockState;
import org.bukkit.event.world.PortalCreateEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

@Mixin(NetherPortalBlock.Size.class)
public abstract class NetherPortalBlock_SizeMixin implements NetherPortalBlockBridge.SizeBridge {

    // @formatter:off
    @Shadow public abstract void placePortalBlocks();
    @Shadow @Final private IWorld world;
    @Shadow private int width;
    @Shadow @Nullable private BlockPos bottomLeft;
    @Shadow @Final private Direction rightDir;
    @Shadow private int height;
    @Shadow @Final private Direction.Axis axis;
    @Shadow protected abstract boolean func_196900_a(net.minecraft.block.BlockState pos);
    @Shadow private int portalBlockCount;
    @Shadow @Final private Direction leftDir;
    // @formatter:on

    List<BlockState> blocks = new ArrayList<>();

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    protected int calculatePortalHeight() {
        this.blocks.clear();
        label56:
        for (this.height = 0; this.height < 21; ++this.height) {
            for (int i = 0; i < this.width; ++i) {
                BlockPos blockpos = this.bottomLeft.offset(this.rightDir, i).up(this.height);
                net.minecraft.block.BlockState blockstate = this.world.getBlockState(blockpos);
                if (!this.func_196900_a(blockstate)) {
                    break label56;
                }

                Block block = blockstate.getBlock();
                if (block == Blocks.NETHER_PORTAL) {
                    ++this.portalBlockCount;
                }

                if (i == 0) {
                    BlockPos framePos = blockpos.offset(this.leftDir);
                    if (!this.world.getBlockState(framePos).isPortalFrame(this.world, framePos)) {
                        break label56;
                    } else {
                        blocks.add(CraftBlock.at(world, framePos).getState());
                    }
                } else if (i == this.width - 1) {
                    BlockPos framePos = blockpos.offset(this.rightDir);
                    if (!this.world.getBlockState(framePos).isPortalFrame(this.world, framePos)) {
                        break label56;
                    } else {
                        blocks.add(CraftBlock.at(world, framePos).getState());
                    }
                }
            }
        }

        for (int j = 0; j < this.width; ++j) {
            BlockPos framePos = this.bottomLeft.offset(this.rightDir, j).up(this.height);
            if (!this.world.getBlockState(framePos).isPortalFrame(this.world, framePos)) {
                this.height = 0;
                break;
            } else {
                blocks.add(CraftBlock.at(world, framePos).getState());
            }
        }

        if (this.height <= 21 && this.height >= 3) {
            return this.height;
        } else {
            this.bottomLeft = null;
            this.width = 0;
            this.height = 0;
            return 0;
        }
    }

    public boolean createPortal() {
        CraftWorld craftWorld = ((WorldBridge) this.world.getWorld()).bridge$getWorld();

        for (int i = 0; i < this.width; ++i) {
            BlockPos blockpos = this.bottomLeft.offset(this.rightDir, i);

            for (int j = 0; j < this.height; ++j) {
                BlockPos blockPos = blockpos.up(j);
                CraftBlockState blockState = CraftBlockState.getBlockState(this.world.getWorld(), blockPos, 18);
                blockState.setData(Blocks.NETHER_PORTAL.getDefaultState().with(NetherPortalBlock.AXIS, this.axis));
                blocks.add(blockState);
            }
        }

        PortalCreateEvent event = new PortalCreateEvent(blocks, craftWorld, null, PortalCreateEvent.CreateReason.FIRE);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return false;
        }
        this.placePortalBlocks();
        return true;
    }

    @Override
    public boolean bridge$createPortal() {
        return createPortal();
    }
}
