package io.izzel.arclight.common.mixin.core.world.level.redstone;

import io.izzel.arclight.common.bridge.core.world.WorldBridge;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.redstone.NeighborUpdater;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v.block.CraftBlock;
import org.bukkit.craftbukkit.v.block.data.CraftBlockData;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import java.util.Locale;

@Mixin(NeighborUpdater.class)
public interface NeighborUpdaterMixin {

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    static void executeUpdate(Level level, BlockState state, BlockPos pos, Block block, BlockPos source, boolean p_230769_) {
        try {
            var cworld = ((WorldBridge) level).bridge$getWorld();
            if (cworld != null) {
                BlockPhysicsEvent event = new BlockPhysicsEvent(CraftBlock.at(level, pos), CraftBlockData.fromData(state), CraftBlock.at(level, source));
                Bukkit.getPluginManager().callEvent(event);

                if (event.isCancelled()) {
                    return;
                }
            }
            state.neighborChanged(level, pos, block, source, p_230769_);
        } catch (StackOverflowError ex) {
            ((WorldBridge) level).bridge$setLastPhysicsProblem(pos.immutable());
            // Spigot End
        } catch (Throwable throwable) {
            CrashReport crashreport = CrashReport.forThrowable(throwable, "Exception while updating neighbours");
            CrashReportCategory crashreportcategory = crashreport.addCategory("Block being updated");
            crashreportcategory.setDetail("Source block type", () -> {
                try {
                    return String.format(Locale.ROOT, "ID #%s (%s // %s)", Registry.BLOCK.getKey(block), block.getDescriptionId(), block.getClass().getCanonicalName());
                } catch (Throwable throwable1) {
                    return "ID #" + Registry.BLOCK.getKey(block);
                }
            });
            CrashReportCategory.populateBlockDetails(crashreportcategory, level, pos, state);
            throw new ReportedException(crashreport);
        }
    }
}
