package io.izzel.arclight.common.mixin.core.advancements;

import io.izzel.arclight.common.bridge.core.advancements.AdvancementHolderBridge;
import net.minecraft.advancements.AdvancementHolder;
import org.bukkit.advancement.Advancement;
import org.bukkit.craftbukkit.v.advancement.CraftAdvancement;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(AdvancementHolder.class)
public class AdvancementHolderMixin implements AdvancementHolderBridge {

    @Override
    public Advancement bridge$getBukkit() {
        return new CraftAdvancement((AdvancementHolder) (Object) this);
    }

    @Unique
    public org.bukkit.advancement.Advancement toBukkit() {
        return new CraftAdvancement((AdvancementHolder)(Object) this);
    }
}
