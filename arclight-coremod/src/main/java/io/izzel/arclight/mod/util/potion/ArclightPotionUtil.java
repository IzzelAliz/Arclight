package io.izzel.arclight.mod.util.potion;

import com.google.common.base.Preconditions;
import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectInstance;
import org.bukkit.Color;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;

import java.util.HashMap;
import java.util.Map;

// todo 在 Registry 事件注册各种 PotionEffectType，删掉这个类
public abstract class ArclightPotionUtil {
    private static final BiMap<PotionType, String> regular;
    private static final BiMap<PotionType, String> upgradeable;
    private static final BiMap<PotionType, String> extendable;
    private static final Map<String, PotionEffectType> effectTypeMap;
    private static final Map<String, PotionEffectType> forgeEffectTypeMap = new HashMap<>();

    static {
        regular = ImmutableBiMap.<PotionType, String>builder().put(PotionType.UNCRAFTABLE, "empty").put(PotionType.WATER, "water").put(PotionType.MUNDANE, "mundane").put(PotionType.THICK, "thick").put(PotionType.AWKWARD, "awkward").put(PotionType.NIGHT_VISION, "night_vision").put(PotionType.INVISIBILITY, "invisibility").put(PotionType.JUMP, "leaping").put(PotionType.FIRE_RESISTANCE, "fire_resistance").put(PotionType.SPEED, "swiftness").put(PotionType.SLOWNESS, "slowness").put(PotionType.WATER_BREATHING, "water_breathing").put(PotionType.INSTANT_HEAL, "healing").put(PotionType.INSTANT_DAMAGE, "harming").put(PotionType.POISON, "poison").put(PotionType.REGEN, "regeneration").put(PotionType.STRENGTH, "strength").put(PotionType.WEAKNESS, "weakness").put(PotionType.LUCK, "luck").put(PotionType.TURTLE_MASTER, "turtle_master").put(PotionType.SLOW_FALLING, "slow_falling").build();
        upgradeable = ImmutableBiMap.<PotionType, String>builder().put(PotionType.JUMP, "strong_leaping").put(PotionType.SPEED, "strong_swiftness").put(PotionType.INSTANT_HEAL, "strong_healing").put(PotionType.INSTANT_DAMAGE, "strong_harming").put(PotionType.POISON, "strong_poison").put(PotionType.REGEN, "strong_regeneration").put(PotionType.STRENGTH, "strong_strength").put(PotionType.SLOWNESS, "strong_slowness").put(PotionType.TURTLE_MASTER, "strong_turtle_master").build();
        extendable = ImmutableBiMap.<PotionType, String>builder().put(PotionType.NIGHT_VISION, "long_night_vision").put(PotionType.INVISIBILITY, "long_invisibility").put(PotionType.JUMP, "long_leaping").put(PotionType.FIRE_RESISTANCE, "long_fire_resistance").put(PotionType.SPEED, "long_swiftness").put(PotionType.SLOWNESS, "long_slowness").put(PotionType.WATER_BREATHING, "long_water_breathing").put(PotionType.POISON, "long_poison").put(PotionType.REGEN, "long_regeneration").put(PotionType.STRENGTH, "long_strength").put(PotionType.WEAKNESS, "long_weakness").put(PotionType.TURTLE_MASTER, "long_turtle_master").put(PotionType.SLOW_FALLING, "long_slow_falling").build();
        effectTypeMap = ImmutableBiMap.<String, PotionEffectType>builder()
                .put("effect.minecraft.speed", PotionEffectType.SPEED)
                .put("effect.minecraft.slow", PotionEffectType.SLOW)
                .put("effect.minecraft.fast_digging", PotionEffectType.FAST_DIGGING)
                .put("effect.minecraft.slow_digging", PotionEffectType.SLOW_DIGGING)
                .put("effect.minecraft.increase_damage", PotionEffectType.INCREASE_DAMAGE)
                .put("effect.minecraft.heal", PotionEffectType.HEAL)
                .put("effect.minecraft.harm", PotionEffectType.HARM)
                .put("effect.minecraft.jump", PotionEffectType.JUMP)
                .put("effect.minecraft.confusion", PotionEffectType.CONFUSION)
                .put("effect.minecraft.regeneration", PotionEffectType.REGENERATION)
                .put("effect.minecraft.damage_resistance", PotionEffectType.DAMAGE_RESISTANCE)
                .put("effect.minecraft.fire_resistance", PotionEffectType.FIRE_RESISTANCE)
                .put("effect.minecraft.water_breathing", PotionEffectType.WATER_BREATHING)
                .put("effect.minecraft.invisibility", PotionEffectType.INVISIBILITY)
                .put("effect.minecraft.blindness", PotionEffectType.BLINDNESS)
                .put("effect.minecraft.night_vision", PotionEffectType.NIGHT_VISION)
                .put("effect.minecraft.hunger", PotionEffectType.HUNGER)
                .put("effect.minecraft.weakness", PotionEffectType.WEAKNESS)
                .put("effect.minecraft.poison", PotionEffectType.POISON)
                .put("effect.minecraft.wither", PotionEffectType.WITHER)
                .put("effect.minecraft.health_boost", PotionEffectType.HEALTH_BOOST)
                .put("effect.minecraft.absorption", PotionEffectType.ABSORPTION)
                .put("effect.minecraft.saturation", PotionEffectType.SATURATION)
                .put("effect.minecraft.glowing", PotionEffectType.GLOWING)
                .put("effect.minecraft.levitation", PotionEffectType.LEVITATION)
                .put("effect.minecraft.luck", PotionEffectType.LUCK)
                .put("effect.minecraft.unluck", PotionEffectType.UNLUCK)
                .put("effect.minecraft.slow_falling", PotionEffectType.SLOW_FALLING)
                .put("effect.minecraft.conduit_power", PotionEffectType.CONDUIT_POWER)
                .put("effect.minecraft.dolphins_grace", PotionEffectType.DOLPHINS_GRACE)
                .put("effect.minecraft.bad_omen", PotionEffectType.BAD_OMEN)
                .put("effect.minecraft.hero_of_the_village", PotionEffectType.HERO_OF_THE_VILLAGE)
                .build();
    }

    private ArclightPotionUtil() {
    }

    public static String fromBukkit(PotionData data) {
        String type;
        if (data.isUpgraded()) {
            type = (String) upgradeable.get(data.getType());
        } else if (data.isExtended()) {
            type = (String) extendable.get(data.getType());
        } else {
            type = (String) regular.get(data.getType());
        }

        Preconditions.checkNotNull(type, "Unknown potion type from data " + data);
        return "minecraft:" + type;
    }

    public static PotionData toBukkit(String type) {
        if (type == null) {
            return new PotionData(PotionType.UNCRAFTABLE, false, false);
        } else {
            if (type.startsWith("minecraft:")) {
                type = type.substring(10);
            }

            PotionType potionType = null;
            potionType = (PotionType) extendable.inverse().get(type);
            if (potionType != null) {
                return new PotionData(potionType, true, false);
            } else {
                potionType = (PotionType) upgradeable.inverse().get(type);
                if (potionType != null) {
                    return new PotionData(potionType, false, true);
                } else {
                    potionType = (PotionType) regular.inverse().get(type);
                    return potionType != null ? new PotionData(potionType, false, false) : new PotionData(PotionType.UNCRAFTABLE, false, false);
                }
            }
        }
    }

    public static EffectInstance fromBukkit(PotionEffect effect) {
        Effect type = Effect.get(effect.getType().getId());
        return new EffectInstance(type, effect.getDuration(), effect.getAmplifier(), effect.isAmbient(), effect.hasParticles());
    }

    public static PotionEffectType toBukkitPotionEffectType(String effectName) {
        return effectTypeMap.getOrDefault(effectName, null);
    }

    public static PotionEffectType forgeToBukkit(EffectInstance effect) {
        try {
            int id = Effect.getId(effect.getPotion());
            String name = effect.getEffectName();
            int colorInt = effect.getPotion().getLiquidColor();
            Color color = Color.fromRGB(colorInt);
            boolean isInstant = effect.getPotion().isInstant();
            return PotionEffectTypeBuilder.build(id, 1.0, name, isInstant, color);
        } catch (Exception e) {
            throw new IllegalStateException("can not convert forge potion effect to bukkit:" + effect.getEffectName());
        }
    }

    //TODO 监听事件，提高性能，不再需要lazy
    public static PotionEffectType forgeToBukkitLazy(EffectInstance effect) {
        final String effectName = effect.getEffectName();
        PotionEffectType type = forgeEffectTypeMap.getOrDefault(effectName, null);
        if (type == null) {
            type = forgeToBukkit(effect);
            forgeEffectTypeMap.put(effectName, type);
        }
        return type;
    }

    public static PotionEffect toBukkit(EffectInstance effect) {
        PotionEffectType type = toBukkitPotionEffectType(effect.getEffectName());
        if (type == null) {
            type = forgeToBukkitLazy(effect);
        }
        int amp = effect.getAmplifier();
        int duration = effect.getDuration();
        boolean ambient = effect.isAmbient();
        boolean particles = effect.doesShowParticles();
        return new PotionEffect(type, duration, amp, ambient, particles);
    }

    public static boolean equals(Effect mobEffect, PotionEffectType type) {
        PotionEffectType typeV = PotionEffectType.getById(Effect.getId(mobEffect));
        return typeV.equals(type);
    }
}
