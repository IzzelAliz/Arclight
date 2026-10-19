package io.izzel.arclight.common.mod.util;

import io.izzel.arclight.common.bridge.core.server.level.ServerPlayerBridge;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.block.CraftBlock;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.craftbukkit.inventory.CraftItemType;
import org.bukkit.event.block.BlockExpEvent;
import org.bukkit.event.inventory.FurnaceExtractEvent;
import org.bukkit.event.inventory.FurnaceSmeltEvent;

/** Event preparation only; native burn/consumeFuel remain responsible for mutations. */
public final class FurnaceEventHooks {
    private FurnaceEventHooks() {
    }

    public static ItemStack smeltResult(ServerLevel level, BlockPos pos, NonNullList<ItemStack> items,
                                        ItemStack input, ItemStack result) {
        FurnaceSmeltEvent event = new FurnaceSmeltEvent(CraftBlock.at(level, pos),
            CraftItemStack.asCraftMirror(input), CraftItemStack.asBukkitCopy(result));
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return null;
        }
        org.bukkit.inventory.ItemStack bukkitResult = event.getResult();
        ItemStack replacement = CraftItemStack.asNMSCopy(bukkitResult);
        ItemStack output = items.get(2);
        if (!replacement.isEmpty() && !output.isEmpty()
            && !CraftItemStack.asCraftMirror(output).isSimilar(bukkitResult)) {
            return null;
        }
        return replacement;
    }

    private record ExperienceContext(BlockPos pos, ServerPlayer player, ItemStack item, int amount) {
    }

    // The extraction request is consumed by the following experience collection.
    private static final ThreadLocal<ExperienceContext> REQUEST = new ThreadLocal<>();
    private static final ThreadLocal<ExperienceContext> ACTIVE = new ThreadLocal<>();

    public static Scope beginExtraction(BlockPos pos, ServerPlayer player, ItemStack item, int amount) {
        return new Scope(REQUEST, new ExperienceContext(pos, player, item, amount));
    }

    public static Scope beginCollection(BlockPos pos) {
        ExperienceContext request = REQUEST.get();
        REQUEST.remove();
        return new Scope(ACTIVE, request != null ? request : new ExperienceContext(pos, null, null, 0));
    }

    public static int experienceToDrop(ServerLevel level, int experience) {
        ExperienceContext context = ACTIVE.get();
        if (context == null) {
            return experience;
        }
        BlockExpEvent event;
        if (context.amount() != 0 && context.player() != null) {
            event = new FurnaceExtractEvent(((ServerPlayerBridge) context.player()).bridge$getBukkitEntity(),
                CraftBlock.at(level, context.pos()), CraftItemType.minecraftToBukkit(context.item().getItem()),
                context.amount(), experience);
        } else {
            event = new BlockExpEvent(CraftBlock.at(level, context.pos()), experience);
        }
        Bukkit.getPluginManager().callEvent(event);
        return event.getExpToDrop();
    }

    public static final class Scope implements AutoCloseable {
        private final ThreadLocal<ExperienceContext> slot;

        private Scope(ThreadLocal<ExperienceContext> slot, ExperienceContext context) {
            this.slot = slot;
            slot.set(context);
        }

        @Override
        public void close() {
            slot.remove();
        }
    }
}
