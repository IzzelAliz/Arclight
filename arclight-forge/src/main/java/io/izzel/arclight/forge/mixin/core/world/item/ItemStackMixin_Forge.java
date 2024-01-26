package io.izzel.arclight.forge.mixin.core.world.item;

import io.izzel.arclight.common.bridge.core.world.item.ItemStackBridge;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.capabilities.CapabilityProvider;
import net.minecraftforge.common.extensions.IForgeItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;

import javax.annotation.Nullable;
import java.util.Locale;

@Mixin(ItemStack.class)
public abstract class ItemStackMixin_Forge extends CapabilityProvider<ItemStack> implements ItemStackBridge, IForgeItemStack {

    // @formatter:off
    @Shadow(remap = false) private CompoundTag capNBT;
    @Mutable @Shadow(remap = false) @Final private net.minecraft.core.Holder.Reference<Item> delegate;
    @Shadow @Deprecated @Nullable private Item item;
    // @formatter:on

    protected ItemStackMixin_Forge(Class<ItemStack> baseClass) {
        super(baseClass);
    }

    @Override
    public CompoundTag bridge$getForgeCaps() {
        return this.serializeCaps();
    }

    @Override
    public void bridge$setForgeCaps(CompoundTag caps) {
        this.capNBT = caps;
        if (caps != null) {
            this.deserializeCaps(caps);
        }
    }

    @Deprecated
    public void setItem(Item item) {
        this.item = item;
        this.delegate = ForgeRegistries.ITEMS.getDelegateOrThrow(item);
    }

    @Override
    public boolean bridge$forge$hasCraftingRemainingItem() {
        return this.hasCraftingRemainingItem();
    }

    @Override
    public ItemStack bridge$forge$getCraftingRemainingItem() {
        return this.getCraftingRemainingItem();
    }

    @Override
    public boolean bridge$forge$canPerformAction(ToolAction action) {
        return this.canPerformAction(net.minecraftforge.common.ToolAction.get(action.name().toLowerCase(Locale.ROOT)));
    }

    @Override
    public AABB bridge$forge$getSweepHitBox(@NotNull Player player, @NotNull Entity target) {
        return this.getSweepHitBox(player, target);
    }

    @Override
    public InteractionResult bridge$forge$onItemUseFirst(UseOnContext context) {
        return onItemUseFirst(context);
    }

    @Override
    public boolean bridge$forge$doesSneakBypassUse(LevelReader level, BlockPos pos, Player player) {
        return doesSneakBypassUse(level, pos, player);
    }
}
