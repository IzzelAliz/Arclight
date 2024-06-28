package io.izzel.arclight.common.mod.util;

public record EntityDamageResult(
    boolean damageOverride,
    float originalDamage,
    float finalDamage,
    float damageOffset,
    float originalArmorDamage,
    float armorDamageOffset,
    boolean helmetHurtCancelled,
    boolean armorHurtCancelled,
    boolean blockingCancelled
) {
}
