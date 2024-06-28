package io.izzel.arclight.common.mixin.core.world.level.levelgen.feature;

import io.izzel.arclight.common.mod.util.ArclightCaptures;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import io.izzel.arclight.mixin.Local;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.EndPlatformFeature;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v.util.BlockStateListPopulator;
import org.bukkit.event.world.PortalCreateEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;

@Mixin(EndPlatformFeature.class)
public class EndPlatformFeatureMixin {

    @Decorate(method = "createEndPlatform", inject = true, at = @At("HEAD"))
    private static void arclight$blockList(ServerLevelAccessor serverLevelAccessor,
                                           @Local(allocate = "blockList") BlockStateListPopulator blockList,
                                           @Local(allocate = "entity") Entity entity) throws Throwable {
        entity = ArclightCaptures.getEndPortalEntity();
        blockList = entity != null ? new BlockStateListPopulator(serverLevelAccessor) : null;
        DecorationOps.blackhole().invoke(blockList, entity);
    }

    @Decorate(method = "createEndPlatform", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/ServerLevelAccessor;getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;"))
    private static BlockState arclight$useBlockList1(ServerLevelAccessor instance, BlockPos pos, @Local(allocate = "blockList") BlockStateListPopulator blockList) throws Throwable {
        if (blockList != null) {
            instance = blockList;
        }
        return (BlockState) DecorationOps.callsite().invoke(instance, pos);
    }

    @Decorate(method = "createEndPlatform", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/ServerLevelAccessor;destroyBlock(Lnet/minecraft/core/BlockPos;ZLnet/minecraft/world/entity/Entity;)Z"))
    private static boolean arclight$useBlockList2(ServerLevelAccessor instance, BlockPos pos, boolean b, Entity entity, @Local(allocate = "blockList") BlockStateListPopulator blockList) throws Throwable {
        if (blockList != null) {
            instance = blockList;
        }
        return (boolean) DecorationOps.callsite().invoke(instance, pos, b, entity);
    }

    @Decorate(method = "createEndPlatform", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/ServerLevelAccessor;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"))
    private static boolean arclight$useBlockList3(ServerLevelAccessor instance, BlockPos pos, BlockState blockState, int i, @Local(allocate = "blockList") BlockStateListPopulator blockList) throws Throwable {
        if (blockList != null) {
            instance = blockList;
        }
        return (boolean) DecorationOps.callsite().invoke(instance, pos, blockState, i);
    }

    @Decorate(method = "createEndPlatform", inject = true, at = @At("RETURN"))
    private static void arclight$portalCreate(ServerLevelAccessor serverLevelAccessor,
                                              @Local(allocate = "blockList") BlockStateListPopulator blockList,
                                              @Local(allocate = "entity") Entity entity) {
        if (blockList != null) {
            var bworld = serverLevelAccessor.getLevel().bridge$getWorld();
            PortalCreateEvent portalEvent = new PortalCreateEvent((List<org.bukkit.block.BlockState>) (List) blockList.getList(), bworld, entity.bridge$getBukkitEntity(), org.bukkit.event.world.PortalCreateEvent.CreateReason.END_PLATFORM);

            Bukkit.getPluginManager().callEvent(portalEvent);
            if (!portalEvent.isCancelled()) {
                blockList.updateList();
            }
        }
    }
}
