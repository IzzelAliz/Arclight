package io.izzel.arclight.common.mod.util;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import io.izzel.arclight.common.bridge.core.world.level.block.SignBlockEditBridge;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SignItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SignBlock;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.event.player.PlayerSignOpenEvent;

/** Opens the sign editor after the ordinary placement transaction succeeds. */
public final class SignPlacementScope {
    private static boolean usingItem;
    private static boolean placing;
    private static BlockPos signPos;
    private static Pending pending;

    private SignPlacementScope() {}

    public static InteractionResult use(UseOnContext context, Operation<InteractionResult> original) {
        if (!(context.getItemInHand().getItem() instanceof SignItem)) return original.call(context);
        usingItem = true;
        pending = null;
        try {
            InteractionResult result = original.call(context);
            finish(result);
            return result;
        } finally {
            usingItem = false;
            pending = null;
        }
    }

    public static InteractionResult place(BlockPlaceContext context, Operation<InteractionResult> original) {
        if (!(context.getItemInHand().getItem() instanceof SignItem)) return original.call(context);
        placing = true;
        try {
            InteractionResult result = original.call(context);
            if (!usingItem) finish(result);
            return result;
        } finally {
            placing = false;
            signPos = null;
            if (!usingItem) pending = null;
        }
    }

    public static boolean tag(BlockPos pos, Level level, Player player, ItemStack stack,
                              Operation<Boolean> original, BlockItem item, BlockState state) {
        signPos = pos.immutable();
        try {
            return original.call(item, pos, level, player, stack, state);
        } finally {
            signPos = null;
        }
    }

    public static void defer(SignBlock block, Player player, SignBlockEntity sign, boolean front,
                             Operation<Void> original, BlockPos pos, Level level, ItemStack stack) {
        if (placing && pos.equals(signPos)) {
            pending = new Pending(level, pos.immutable(), player, front, original);
        } else {
            original.call(block, player, sign, front);
        }
    }

    private static void finish(InteractionResult result) {
        Pending editor = pending;
        pending = null;
        if (editor != null && result instanceof InteractionResult.Success success && success.wasItemInteraction()) {
            editor.open();
        }
    }

    private record Pending(Level level, BlockPos pos, Player player, boolean front, Operation<Void> original) {
        private void open() {
            if (!level.isClientSide() && player != null
                && level.getBlockEntity(pos) instanceof SignBlockEntity sign
                && level.getBlockState(pos).getBlock() instanceof SignBlock block) {
                ((SignBlockEditBridge) block).bridge$openTextEdit(player, sign, front,
                    PlayerSignOpenEvent.Cause.PLACE, original);
            }
        }
    }
}
