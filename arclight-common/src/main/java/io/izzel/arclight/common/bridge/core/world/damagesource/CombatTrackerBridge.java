package io.izzel.arclight.common.bridge.core.world.damagesource;

import io.papermc.paper.world.damagesource.PaperCombatTrackerWrapper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.damagesource.CombatEntry;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface CombatTrackerBridge {

    void bridge$setDeathMessage(Component component);

    PaperCombatTrackerWrapper bridge$getPaperCombatTracker();

    List<CombatEntry> bridge$getEntries();

    LivingEntity bridge$getMob();

    boolean bridge$isInCombat();

    boolean bridge$isTakingDamage();

    @Nullable
    CombatEntry bridge$getMostSignificantFall();

    /** Records the supplied entry unchanged, using the native damage-recording state transition. */
    void bridge$recordDamageAndCheckCombatState(CombatEntry entry);

    /** Unconditionally performs the native combat-reset transition, including clearing history. */
    void bridge$resetCombatState();

    default void recordDamageAndCheckCombatState(net.minecraft.world.damagesource.CombatEntry entry) {
        this.bridge$recordDamageAndCheckCombatState(entry);
    }

    default void resetCombatState() {
        this.bridge$resetCombatState();
    }

}
