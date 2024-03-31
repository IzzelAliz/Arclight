package io.izzel.arclight.neoforge.mixin.core.world.item;

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
import net.neoforged.neoforge.attachment.AttachmentHolder;
import net.neoforged.neoforge.attachment.AttachmentUtils;
import net.neoforged.neoforge.common.extensions.IItemStackExtension;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;

import javax.annotation.Nullable;
import java.util.Locale;

@SuppressWarnings("MixinSuperClass")
@Mixin(ItemStack.class)
public abstract class ItemStackMixin_NeoForge extends AttachmentHolder implements ItemStackBridge, IItemStackExtension {

    // @formatter:off
    @Mutable @Shadow @Final @Deprecated @Nullable private Item item;
    // @formatter:on

    @Override
    public CompoundTag bridge$getForgeCaps() {
        return this.serializeAttachments();
    }

    @Override
    public void bridge$setForgeCaps(CompoundTag caps) {
        if (caps != null) {
            this.deserializeAttachments(caps);
        }
    }

    @Deprecated
    public void setItem(Item item) {
        this.item = item;
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
        return this.canPerformAction(net.neoforged.neoforge.common.ToolAction.get(action.name().toLowerCase(Locale.ROOT)));
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

    @Override
    public void bridge$platform$copyAdditionalFrom(ItemStack from) {
        AttachmentUtils.copyStackAttachments(from, (ItemStack) (Object) this);
    }
}
