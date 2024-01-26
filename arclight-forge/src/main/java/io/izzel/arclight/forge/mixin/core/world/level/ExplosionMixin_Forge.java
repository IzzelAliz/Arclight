package io.izzel.arclight.forge.mixin.core.world.level;

import io.izzel.arclight.common.bridge.core.world.ExplosionBridge;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.ForgeEventFactory;
import org.spongepowered.asm.mixin.Mixin;

import java.util.List;

@Mixin(Explosion.class)
public abstract class ExplosionMixin_Forge implements ExplosionBridge {

    @Override
    public void bridge$forge$onExplosionDetonate(Level level, Explosion explosion, List<Entity> list, double diameter) {
        ForgeEventFactory.onExplosionDetonate(level, explosion, list, diameter);
    }
}
