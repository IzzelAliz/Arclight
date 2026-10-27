package io.izzel.arclight.common.mixin.core.world.level.block;

import io.izzel.arclight.common.mod.server.block.DispenserBlockHooks;
import io.izzel.arclight.common.mod.server.block.DispenserBlockHooks.Context;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.core.dispenser.DispenseItemBehavior;
import net.minecraft.world.level.block.DropperBlock;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.block.CraftBlock;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.craftbukkit.util.CraftVector;
import org.bukkit.event.block.BlockDispenseEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(DefaultDispenseItemBehavior.class)
public abstract class DefaultDispenseItemBehaviorMixin {

    // Only execute's native split(1) has an inventory stack to refund. The static five-argument
    // spawnItem is also used by the remainder overflow path and other callers; never infer a
    // BlockSource from its Position or synthesize the CraftBukkit boolean overload there.
    @Unique private static Context arclight$context;


    @Decorate(method = "execute", at = @At(value = "INVOKE", target = "Lnet/minecraft/core/dispenser/DefaultDispenseItemBehavior;spawnItem(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;ILnet/minecraft/core/Direction;Lnet/minecraft/core/Position;)V"))
    private void arclight$defaultCall(Level level, ItemStack single, int accuracy, Direction direction, Position position,
                                      BlockSource source, ItemStack original) throws Throwable {
        Context parent = arclight$context;
        Context context = new Context(parent, source, original);
        arclight$context = context;
        try {
            DecorationOps.callsite().invoke(level, single, accuracy, direction, position);
        } finally {
            try {
                if (context.refund) {
                    original.grow(1);
                }
            } finally {
                // A grow hook may itself throw; never leak this call's source into later dispenses.
                arclight$context = parent;
            }
        }
        // A cancelled/replaced default action returns the restored original to the caller.
        // cancel() rewrites to execute's ARETURN, so it MUST be outside the Java finally.
        if (context.refund) {
            DecorationOps.cancel().invoke(original);
            return;
        }
    }

    @Decorate(method = "spawnItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"))
    private static boolean arclight$defaultEvent(Level level, Entity entity) throws Throwable {
        Context context = arclight$context;
        boolean skip = false;
        boolean refundAfterNativeAdd = false;
        if (context != null && context.parent == null && entity instanceof ItemEntity itemEntity
            && !itemEntity.getItem().isEmpty()) {
            BlockSource source = context.source;
            CraftItemStack originalItem = CraftItemStack.asCraftMirror(itemEntity.getItem());
            BlockDispenseEvent event = new BlockDispenseEvent(CraftBlock.at(level, source.pos()),
                originalItem.clone(), CraftVector.toBukkit(itemEntity.getDeltaMovement()));
            if (!DispenserBlockHooks.isEventFired() && !context.inEvent) {
                context.inEvent = true;
                try {
                    Bukkit.getPluginManager().callEvent(event);
                } finally {
                    context.inEvent = false;
                }
            }
            if (event.isCancelled()) {
                context.refund = true;
                skip = true;
            } else {
                ItemStack eventStack = CraftItemStack.asNMSCopy(event.getItem());
                itemEntity.setItem(eventStack);
                itemEntity.setDeltaMovement(CraftVector.toVec3(event.getVelocity()));
                if (!(source.state().getBlock() instanceof DropperBlock)
                    && event.getItem().getType() != originalItem.getType()) {
                    // Use a second copy as fixed CB does: handler mutations must not edit the drop entity.
                    ItemStack replacement = CraftItemStack.asNMSCopy(event.getItem());
                    DispenseItemBehavior behavior = ((DispenserBlock) source.state().getBlock())
                        .getDispenseMethod(source.level(), replacement);
                    if (behavior != DispenseItemBehavior.NOOP
                        && behavior.getClass() != DefaultDispenseItemBehavior.class) {
                        behavior.dispense(source, replacement);
                        // Fixed CB returns false only after a normally completed replacement call.
                        context.refund = true;
                        skip = true;
                    } else {
                        // Keep this callsite's original add chain; native false still completes normally.
                        refundAfterNativeAdd = true;
                    }
                }
            }
        }
        if (skip) {
            // The original execute receives the refund through Context. Decorate.cancel()
            // rewrites to this void spawnItem method's RETURN; caller cleanup stays in finally.
            // A typed direct return keeps IRETURN immediately after invoke for the collector.
            return (boolean) DecorationOps.cancel().invoke();
        }
        boolean added = (boolean) DecorationOps.callsite().invoke(level, entity);
        if (refundAfterNativeAdd) {
            context.refund = true;
        }
        return added;
    }
}
