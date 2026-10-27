package io.izzel.arclight.common.mixin.core.world.item;

import io.izzel.arclight.common.bridge.core.world.entity.LivingEntityBridge;
import io.izzel.arclight.common.bridge.core.world.entity.player.PlayerBridge;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import io.izzel.arclight.mixin.Local;
import io.papermc.paper.adventure.PaperAdventure;
import io.papermc.paper.event.player.PlayerNameEntityEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.NameTagItem;
import org.bukkit.craftbukkit.entity.CraftLivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(NameTagItem.class)
public abstract class NameTagItemMixin {

    @Decorate(method = "interactLivingEntity(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/InteractionResult;",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;setCustomName(Lnet/minecraft/network/chat/Component;)V"), require = 1, allow = 1)
    private void arclight$playerNameEntity(LivingEntity instance, Component customName,
                                         ItemStack stack, Player player, LivingEntity target, InteractionHand hand,
                                         @Local(allocate = "arclightNameTagPersistent") boolean persistent) throws Throwable {
        var event = new PlayerNameEntityEvent((org.bukkit.entity.Player) ((PlayerBridge) player).bridge$getBukkitEntity(),
            ((LivingEntityBridge) instance).bridge$getBukkitEntity(), PaperAdventure.asAdventure(customName), true);
        if (!event.callEvent()) {
            DecorationOps.cancel().invoke((InteractionResult) InteractionResult.PASS);
            return;
        }

        // Write the selected target back for the native Mob check below as well as the name call.
        target = ((CraftLivingEntity) event.getEntity()).getHandle();
        persistent = event.isPersistent();
        DecorationOps.blackhole().invoke(target, persistent);
        Component selectedName = event.getName() != null ? PaperAdventure.asVanilla(event.getName()) : null;
        DecorationOps.callsite().invoke(target, selectedName);
    }

    @Decorate(method = "interactLivingEntity(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/InteractionResult;",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Mob;setPersistenceRequired()V"), require = 1, allow = 1)
    private void arclight$nameTagPersistence(Mob mob,
                                           @Local(allocate = "arclightNameTagPersistent") boolean persistent) throws Throwable {
        if (persistent) {
            DecorationOps.callsite().invoke(mob);
        }
    }
}
