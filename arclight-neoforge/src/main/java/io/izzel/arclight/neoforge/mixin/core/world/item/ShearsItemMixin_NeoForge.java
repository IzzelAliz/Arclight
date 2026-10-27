package io.izzel.arclight.neoforge.mixin.core.world.item;

import io.izzel.arclight.common.bridge.core.world.entity.player.PlayerBridge;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import io.izzel.arclight.mixin.Local;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShearsItem;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.IShearable;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;

@Mixin(ShearsItem.class)
public abstract class ShearsItemMixin_NeoForge {

    @Decorate(method = "interactLivingEntity", at = @At(value = "INVOKE", remap = false, target = "Lnet/neoforged/neoforge/common/IShearable;onSheared(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)Ljava/util/List;"))
    private List<ItemStack> arclight$onSheared(IShearable shearable, Player player, ItemStack tool, Level level, BlockPos pos, @Local(ordinal = 0) InteractionHand hand) throws Throwable {
        if (((PlayerBridge) player).bridge$isCopperShearCancelled((Entity) shearable)) {
            return (List<ItemStack>) DecorationOps.cancel().invoke((InteractionResult) InteractionResult.PASS);
        }
        List<ItemStack> drops = (List<ItemStack>) DecorationOps.callsite().invoke(shearable, player, tool, level, pos);
        if (!(level instanceof ServerLevel)) {
            return drops;
        }
        var event = CraftEventFactory.handlePlayerShearEntityEvent(player, (Entity) shearable, tool, hand, drops);
        if (event != null && event.isCancelled()) {
            return (List<ItemStack>) DecorationOps.cancel().invoke((InteractionResult) InteractionResult.PASS);
        }
        return event == null ? drops : CraftItemStack.asNMSCopy(event.getDrops());
    }
}
