package io.izzel.arclight.neoforge.mixin.core.world.level.storage.loot;

import io.izzel.arclight.common.bridge.core.world.storage.loot.LootContextBridge;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.storage.loot.LootContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(LootContext.class)
public abstract class LootContextMixin_NeoForge implements LootContextBridge {

    // @formatter:off
    @Shadow(remap = false) public abstract int getLootingModifier();
    // @formatter:on

    @Override
    public int bridge$forge$getLootingModifier(Entity entity) {
        return this.getLootingModifier();
    }
}
