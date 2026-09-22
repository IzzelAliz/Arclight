package io.izzel.arclight.common.mod.util;

import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import java.util.Map;

public final class BulkEffectRemovalContext {
    public final Map<Holder<MobEffect>, MobEffectInstance> originalEffects;
    public final EntityPotionEffectEvent.Cause cause;

    public BulkEffectRemovalContext(Map<Holder<MobEffect>, MobEffectInstance> effects, EntityPotionEffectEvent.Cause cause) {
        this.originalEffects = effects;
        this.cause = cause;
    }
}
