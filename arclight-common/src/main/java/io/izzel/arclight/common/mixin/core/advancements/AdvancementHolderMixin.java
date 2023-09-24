package io.izzel.arclight.common.mixin.core.advancements;

import io.izzel.arclight.common.bridge.core.advancement.AdvancementBridge;
import net.minecraft.advancements.AdvancementHolder;
import org.bukkit.advancement.Advancement;
import org.bukkit.craftbukkit.v.advancement.CraftAdvancement;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(AdvancementHolder.class)
public class AdvancementHolderMixin implements AdvancementBridge {

    @Override
    public Advancement bridge$getBukkit() {
        return new CraftAdvancement((AdvancementHolder) (Object) this);
    }
}
