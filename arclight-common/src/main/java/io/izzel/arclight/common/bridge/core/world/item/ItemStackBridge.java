package io.izzel.arclight.common.bridge.core.world.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.LevelReader;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public interface ItemStackBridge {

    final class Serialization {
        public static final com.mojang.serialization.JsonOps SNBT_JSON_OPS = new com.mojang.serialization.JsonOps(false) {};

        private Serialization() {}
    }

    default void restorePatch(DataComponentPatch datacomponentpatch) {
        this.arclight$restorePatch(datacomponentpatch);
    }

    @Deprecated
    default void setItem(Item item) {
        this.arclight$setItem(item);
    }

    default org.bukkit.inventory.ItemStack asBukkitCopy() {
        throw new UnsupportedOperationException("Implemented by ItemStackMixin");
    }

    default org.bukkit.inventory.ItemStack asBukkitMirror() {
        throw new UnsupportedOperationException("Implemented by ItemStackMixin");
    }

    void arclight$restorePatch(DataComponentPatch datacomponentpatch);

    void arclight$setItem(Item item);

    void bridge$hurtAndBreak(int amount, LivingEntity owner, @Nullable EquipmentSlot slot, boolean force);

    default void bridge$platform$hurtAndBreak(int amount, ServerLevel level, LivingEntity owner, Consumer<Item> onBreak) {
        ((ItemStack) this).hurtAndBreak(amount, level, owner instanceof ServerPlayer player ? player : null, onBreak);
    }

    default boolean bridge$canSetItem(Holder<Item> item) {
        return true;
    }

    default InteractionResult bridge$forge$onItemUseFirst(UseOnContext context) {
        return InteractionResult.PASS;
    }

    default boolean bridge$forge$doesSneakBypassUse(LevelReader level, BlockPos pos, Player player) {
        return false;
    }

    default void hurtAndBreak(int amount, net.minecraft.world.entity.LivingEntity owner, net.minecraft.world.entity.EquipmentSlot slot, boolean force) {
        this.bridge$hurtAndBreak(amount, owner, slot, force);
    }


    static com.mojang.serialization.Codec<net.minecraft.nbt.CompoundTag> customDataCodec() {
        return new com.mojang.serialization.Codec<>() {
            @Override
            public <T> com.mojang.serialization.DataResult<com.mojang.datafixers.util.Pair<net.minecraft.nbt.CompoundTag, T>> decode(
                    com.mojang.serialization.DynamicOps<T> ops, T input) {
                return com.mojang.serialization.Codec.withAlternative(net.minecraft.nbt.CompoundTag.CODEC,
                    net.minecraft.nbt.TagParser.FLATTENED_CODEC).decode(ops, input);
            }

            @Override
            public <T> com.mojang.serialization.DataResult<T> encode(net.minecraft.nbt.CompoundTag input,
                    com.mojang.serialization.DynamicOps<T> ops, T prefix) {
                com.mojang.serialization.DynamicOps<?> format = ops;
                while (format instanceof net.minecraft.resources.DelegatingOps<?> delegated) {
                    format = delegated.delegate;
                }
                return (format == Serialization.SNBT_JSON_OPS
                    ? net.minecraft.nbt.TagParser.FLATTENED_CODEC : net.minecraft.nbt.CompoundTag.CODEC).encode(input, ops, prefix);
            }
        };
    }
}
