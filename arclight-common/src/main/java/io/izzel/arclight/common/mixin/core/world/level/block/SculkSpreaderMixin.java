package io.izzel.arclight.common.mixin.core.world.level.block;

import io.izzel.arclight.common.bridge.core.world.level.block.SculkSpreaderBridge;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SculkSpreader;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v.block.CraftBlock;
import org.bukkit.event.block.SculkBloomEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SculkSpreader.class)
public abstract class SculkSpreaderMixin implements SculkSpreaderBridge {

    // @formatter:off
    @Shadow public abstract boolean isWorldGeneration();
    // @formatter:on

    private transient Level arclight$level;

    @Override
    public void bridge$setLevel(Level level) {
        this.arclight$level = level;
    }

    @Inject(method = "addCursor", cancellable = true, at = @At(value = "INVOKE", remap = false, target = "Ljava/util/List;add(Ljava/lang/Object;)Z"))
    private void arclight$bloomEvent(SculkSpreader.ChargeCursor cursor, CallbackInfo ci) {
        if (!isWorldGeneration() && arclight$level != null) {
            var bukkitBlock = CraftBlock.at(arclight$level, cursor.pos);
            var event = new SculkBloomEvent(bukkitBlock, cursor.getCharge());
            Bukkit.getPluginManager().callEvent(event);
            if (event.isCancelled()) {
                ci.cancel();
            }
            cursor.charge = event.getCharge();
        }
    }
}
