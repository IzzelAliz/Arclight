package io.izzel.arclight.common.mixin.core.world.level.block.entity;

import io.izzel.arclight.common.bridge.core.world.level.block.SculkSpreaderBridge;
import io.izzel.arclight.common.bridge.core.tileentity.SculkCatalystListenerBridge;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SculkSpreader;
import net.minecraft.world.level.block.entity.SculkCatalystBlockEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(SculkCatalystBlockEntity.CatalystListener.class)
public class SculkCatalystBlockEntity_CatalystListenerMixin implements SculkCatalystListenerBridge {

    @Shadow @Final SculkSpreader sculkSpreader;

    @Override
    public void bridge$setLevel(Level level) {
        ((SculkSpreaderBridge) this.sculkSpreader).bridge$setLevel(level);
    }
}
