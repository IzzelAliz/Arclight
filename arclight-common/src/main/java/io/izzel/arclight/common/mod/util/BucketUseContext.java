package io.izzel.arclight.common.mod.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

/** Invocation-local bucket state, including nested plugin callbacks. */
public final class BucketUseContext {
    public Direction direction;
    public BlockPos click;
    public InteractionHand hand;
    public ItemStack stack;
    public org.bukkit.inventory.ItemStack captureItem;
}
