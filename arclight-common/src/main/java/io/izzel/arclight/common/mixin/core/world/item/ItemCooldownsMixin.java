package io.izzel.arclight.common.mixin.core.world.item;

import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import io.papermc.paper.event.player.PlayerItemGroupCooldownEvent;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemCooldowns;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Map;

@Mixin(ItemCooldowns.class)
public abstract class ItemCooldownsMixin {

    @Shadow @Final private Map<Identifier, ItemCooldowns.CooldownInstance> cooldowns;
    @Shadow private int tickCount;

    @Unique private ItemStack arclight$cooldownItem;

    @Decorate(method = "addCooldown(Lnet/minecraft/world/item/ItemStack;I)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemCooldowns;addCooldown(Lnet/minecraft/resources/Identifier;I)V"))
    private void arclight$itemCooldown(ItemCooldowns cooldowns, Identifier group, int ticks, ItemStack item, int duration) throws Throwable {
        ItemStack previous = this.arclight$cooldownItem;
        this.arclight$cooldownItem = item;
        try {
            DecorationOps.callsite().invoke(cooldowns, group, ticks);
        } finally {
            this.arclight$cooldownItem = previous;
        }
    }

    @Decorate(method = "addCooldown(Lnet/minecraft/resources/Identifier;I)V", inject = true, at = @At("HEAD"))
    private void arclight$cooldownEvent(Identifier group, int ticks) throws Throwable {
        // Claim the item before dispatch so listener-initiated group calls remain group events.
        ItemStack item = this.arclight$cooldownItem;
        this.arclight$cooldownItem = null;
        PlayerItemGroupCooldownEvent event = this.arclight$createCooldownEvent(item, group, ticks);
        if (event != null) {
            if (!event.callEvent()) {
                this.arclight$resyncCooldown(group);
                DecorationOps.cancel().invoke();
                return;
            }
            ticks = event.getCooldown();
            DecorationOps.blackhole().invoke(ticks);
        }
    }

    protected PlayerItemGroupCooldownEvent arclight$createCooldownEvent(ItemStack item, Identifier group, int ticks) {
        return null;
    }

    protected void arclight$resyncCooldown(Identifier group) {
    }

    protected int arclight$currentCooldown(Identifier group) {
        ItemCooldowns.CooldownInstance cooldown = this.cooldowns.get(group);
        return cooldown == null ? 0 : Math.max(0, cooldown.endTime() - this.tickCount);
    }
}
