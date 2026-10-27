package io.izzel.arclight.common.mod.util;

import net.minecraft.world.entity.ConversionParams;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.npc.villager.Villager;
import org.bukkit.craftbukkit.event.CraftEventFactory;

/** The actual lightning conversion callback, not an ambient entity-creation scope. */
public final class VillagerZapConversion<T extends Mob> implements ConversionParams.AfterConversion<T> {

    private final Villager source;
    private final LightningBolt lightning;
    private final ConversionParams.AfterConversion<T> original;
    private final Runnable log;

    public VillagerZapConversion(Villager source, LightningBolt lightning,
            ConversionParams.AfterConversion<T> original, Runnable log) {
        this.source = source;
        this.lightning = lightning;
        this.original = original;
        this.log = log;
    }

    public boolean callEvent(Mob replacement) {
        return !CraftEventFactory.callEntityZapEvent(this.source, this.lightning, replacement).isCancelled();
    }

    @Override
    public void finalizeConversion(T replacement) {
        if (this.log != null) {
            this.log.run();
        }
        // Includes native finalization, Neo's onLivingConvert, persistence and POI release.
        this.original.finalizeConversion(replacement);
    }
}
