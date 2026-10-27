package io.izzel.arclight.common.mixin.core.world.level.block;

import io.izzel.arclight.common.bridge.core.entity.EntityBridge;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.TripWireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.block.CraftBlock;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.bukkit.event.Cancellable;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityInteractEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;

@Mixin(TripWireBlock.class)
public abstract class TripWireBlockMixin {

    // @formatter:off
    @Shadow @Final public static BooleanProperty POWERED;
    // @formatter:on

    @Decorate(method = "checkPressed(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Ljava/util/List;)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"),
        require = 1, allow = 1)
    private boolean arclight$entityInteract(Level worldIn, BlockPos pos, BlockState blockstate, int flags,
                                           Level originalLevel, BlockPos originalPos, List<? extends Entity> entities) throws Throwable {
        // This write is reached only for a changed signal. Keep the native
        // candidate list: entityInside passes List.of(entity), tick queries it.
        if (blockstate.getValue(POWERED) && blockstate.getValue(TripWireBlock.ATTACHED)) {
            org.bukkit.block.Block block = CraftBlock.at(worldIn, pos);
            boolean allowed = false;

            // If all of the events are cancelled block the tripwire trigger, else allow
            for (Object object : entities) {
                if (object != null) {
                    Cancellable cancellable;

                    if (object instanceof Player) {
                        cancellable = CraftEventFactory.callPlayerInteractEvent((Player) object, Action.PHYSICAL, pos, null, null, null);
                    } else if (object instanceof Entity) {
                        cancellable = new EntityInteractEvent(((EntityBridge) object).bridge$getBukkitEntity(), block);
                        Bukkit.getPluginManager().callEvent((EntityInteractEvent) cancellable);
                    } else {
                        continue;
                    }

                    if (!cancellable.isCancelled()) {
                        allowed = true;
                        break;
                    }
                }
            }

            if (!allowed) {
                // The host is void: veto both its write and native update/tick tail.
                return (boolean) DecorationOps.cancel().invoke();
            }
        }

        return (boolean) DecorationOps.callsite().invoke(worldIn, pos, blockstate, flags);
    }
}
