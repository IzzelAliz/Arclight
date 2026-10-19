package io.izzel.arclight.common.mixin.core.world.level.block.entity.vault;

import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.vault.VaultConfig;
import net.minecraft.world.level.block.entity.vault.VaultServerData;
import net.minecraft.world.level.block.entity.vault.VaultSharedData;
import net.minecraft.world.level.block.entity.vault.VaultState;
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.event.block.BlockDispenseLootEvent;
import org.bukkit.event.block.VaultDisplayItemEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;

@Mixin(targets = "net.minecraft.world.level.block.entity.vault.VaultBlockEntity$Server")
public abstract class VaultBlockEntity_ServerMixin {

    // This call is inside the native nonempty-loot gate, after awardStat and key consumption.
    // A veto must exit tryInsertKey, not merely skip unlock and fall into the rewarded tail.
    @Decorate(method = "tryInsertKey", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/entity/vault/VaultBlockEntity$Server;unlock(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/entity/vault/VaultConfig;Lnet/minecraft/world/level/block/entity/vault/VaultServerData;Lnet/minecraft/world/level/block/entity/vault/VaultSharedData;Ljava/util/List;)V"))
    private static void arclight$dispenseLoot(ServerLevel level, BlockState state, BlockPos pos,
                                             VaultConfig config, VaultServerData serverData,
                                             VaultSharedData sharedData, List<ItemStack> loot,
                                             ServerLevel hostLevel, BlockPos hostPos, BlockState hostState,
                                             VaultConfig hostConfig, VaultServerData hostServerData,
                                             VaultSharedData hostSharedData, Player player, ItemStack key) throws Throwable {
        BlockDispenseLootEvent event = CraftEventFactory.callBlockDispenseLootEvent(level, pos, player, loot);
        if (event.isCancelled()) {
            // Fixed CraftBukkit intentionally keeps the already awarded stat and consumed key.
            DecorationOps.cancel().invoke();
            return;
        }
        List<ItemStack> replacement = event.getDispensedLoot().stream().map(CraftItemStack::asNMSCopy).toList();
        // Plugin-empty loot still unlocks and records the rewarded player; do not retest emptiness.
        DecorationOps.callsite().invoke(level, state, pos, config, serverData, sharedData, replacement);
    }

    // Only the second setter belongs to random selection. The first clears an inactive vault.
    // Unlock/ejection/INACTIVE.onEnter setters are deliberately outside this event entry.
    @Decorate(method = "cycleDisplayItemFromLootTable", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/entity/vault/VaultSharedData;setDisplayItem(Lnet/minecraft/world/item/ItemStack;)V", ordinal = 1))
    private static void arclight$displayItem(VaultSharedData sharedData, ItemStack selected,
                                            ServerLevel level, VaultState state, VaultConfig config,
                                            VaultSharedData hostSharedData, BlockPos pos) throws Throwable {
        VaultDisplayItemEvent event = CraftEventFactory.callVaultDisplayItemEvent(level, pos, selected);
        if (event.isCancelled()) {
            DecorationOps.cancel().invoke();
            return;
        }
        DecorationOps.callsite().invoke(sharedData, CraftItemStack.asNMSCopy(event.getDisplayItem()));
    }
}
