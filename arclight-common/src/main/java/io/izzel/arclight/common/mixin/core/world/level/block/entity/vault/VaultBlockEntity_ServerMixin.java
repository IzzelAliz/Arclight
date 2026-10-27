package io.izzel.arclight.common.mixin.core.world.level.block.entity.vault;

import io.izzel.arclight.common.bridge.core.world.entity.player.PlayerBridge;
import io.izzel.arclight.common.bridge.core.world.level.WorldBridge;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import io.izzel.arclight.mixin.Local;
import io.papermc.paper.event.block.VaultChangeStateEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.vault.VaultConfig;
import net.minecraft.world.level.block.entity.vault.VaultServerData;
import net.minecraft.world.level.block.entity.vault.VaultSharedData;
import net.minecraft.world.level.block.entity.vault.VaultState;
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.block.data.type.Vault;
import org.bukkit.craftbukkit.block.CraftBlock;
import org.bukkit.craftbukkit.block.data.CraftBlockData;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.event.block.BlockDispenseLootEvent;
import org.bukkit.event.block.VaultDisplayItemEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Mixin(targets = "net.minecraft.world.level.block.entity.vault.VaultBlockEntity$Server")
public abstract class VaultBlockEntity_ServerMixin {

    // Native unlock has no player parameter. Carry it only through the accepted unlock call.
    @Unique
    private static Map.Entry<VaultSharedData, Player> arclight$unlockingPlayer;

    // This call is inside the native nonempty-loot gate, after awardStat and key consumption.
    // A veto must exit tryInsertKey, not merely skip unlock and fall into the rewarded tail.
    @Decorate(method = "tryInsertKey", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/entity/vault/VaultBlockEntity$Server;unlock(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/entity/vault/VaultConfig;Lnet/minecraft/world/level/block/entity/vault/VaultServerData;Lnet/minecraft/world/level/block/entity/vault/VaultSharedData;Ljava/util/List;)V"))
    private static void arclight$dispenseLoot(ServerLevel level, BlockState state, BlockPos pos,
                                             VaultConfig config, VaultServerData serverData,
                                             VaultSharedData sharedData, List<ItemStack> loot,
                                             ServerLevel hostLevel, BlockPos hostPos, BlockState hostState,
                                             VaultConfig hostConfig, VaultServerData hostServerData,
                                             VaultSharedData hostSharedData, Player player, ItemStack key) throws Throwable {
        BlockDispenseLootEvent event = CraftEventFactory.callBlockDispenseLootEvent(level, pos, player, loot, level.getServer().reloadableRegistries().getLootTable(config.lootTable()));
        if (event.isCancelled()) {
            // Fixed CraftBukkit intentionally keeps the already awarded stat and consumed key.
            DecorationOps.cancel().invoke();
            return;
        }
        List<ItemStack> replacement = event.getDispensedLoot().stream().map(CraftItemStack::asNMSCopy).toList();
        // Plugin-empty loot still unlocks and records the rewarded player; do not retest emptiness.
        var previous = arclight$unlockingPlayer;
        arclight$unlockingPlayer = Map.entry(sharedData, player);
        try {
            DecorationOps.callsite().invoke(level, state, pos, config, serverData, sharedData, replacement);
        } finally {
            arclight$unlockingPlayer = previous;
        }
    }

    @Decorate(method = "setVaultState(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/entity/vault/VaultConfig;Lnet/minecraft/world/level/block/entity/vault/VaultSharedData;)V",
        inject = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"), require = 1, allow = 1)
    private static void arclight$changeState(ServerLevel level, BlockPos pos, BlockState currentBlockState,
                                            BlockState newBlockState, VaultConfig config, VaultSharedData sharedData,
                                            @Local(ordinal = 0) VaultState currentState,
                                            @Local(ordinal = 1) VaultState newState) throws Throwable {
        var unlockingPlayer = arclight$unlockingPlayer;
        // Claim before callbacks: an unrelated transition must not inherit this unlock's player.
        arclight$unlockingPlayer = null;
        org.bukkit.entity.Player associatedPlayer = null;
        if (unlockingPlayer != null && unlockingPlayer.getKey() == sharedData) {
            associatedPlayer = (org.bukkit.entity.Player) ((PlayerBridge) unlockingPlayer.getValue()).bridge$getBukkitEntity();
        } else if (newState == VaultState.ACTIVE) {
            Set<UUID> connectedPlayers = sharedData.getConnectedPlayers();
            if (!connectedPlayers.isEmpty()) {
                associatedPlayer = ((WorldBridge) level).bridge$getServer().getPlayer(connectedPlayers.iterator().next());
            }
        }
        VaultChangeStateEvent event = new VaultChangeStateEvent(CraftBlock.at(level, pos), associatedPlayer,
            CraftBlockData.fromVanilla(currentState, Vault.State.class),
            CraftBlockData.fromVanilla(newState, Vault.State.class));
        if (!event.callEvent()) {
            // Veto only this setter: keep the caller's reward, pause and dirty-update tails.
            DecorationOps.cancel().invoke();
            return;
        }
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
