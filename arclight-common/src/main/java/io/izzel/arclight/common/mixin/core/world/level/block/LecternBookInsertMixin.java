package io.izzel.arclight.common.mixin.core.world.level.block;

import io.izzel.arclight.common.bridge.core.server.level.ServerPlayerBridge;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import io.izzel.arclight.mixin.Local;
import io.papermc.paper.event.player.PlayerInsertLecternBookEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LecternBlock;
import org.bukkit.craftbukkit.block.CraftBlock;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LecternBlock.class)
public class LecternBookInsertMixin {

    @Decorate(method = "placeBook(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/item/ItemStack;)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;consumeAndReturn(ILnet/minecraft/world/entity/LivingEntity;)Lnet/minecraft/world/item/ItemStack;"), require = 1, allow = 1)
    private static ItemStack arclight$insertBook(ItemStack book, int amount, LivingEntity sourceEntity,
                                                @Local(ordinal = 0) Level level, @Local(ordinal = 0) BlockPos pos) throws Throwable {
        ItemStack eventBook = null;
        if (sourceEntity instanceof ServerPlayer player) {
            var event = new PlayerInsertLecternBookEvent(((ServerPlayerBridge) player).bridge$getBukkitEntity(),
                CraftBlock.at(level, pos), CraftItemStack.asCraftMirror(book.copyWithCount(1)));
            if (!event.callEvent()) {
                // Cancel the void placeBook host before consumption, setBook and its effects.
                return (ItemStack) DecorationOps.cancel().invoke();
            }
            eventBook = CraftItemStack.unwrap(event.getBook());
        }
        // Keep native/mod consumption (including creative and nullable-source semantics).
        ItemStack consumedBook = (ItemStack) DecorationOps.callsite().invoke(book, amount, sourceEntity);
        return eventBook == null ? consumedBook : eventBook;
    }
}
