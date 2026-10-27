package io.izzel.arclight.common.mixin.core.world.damagesource;

import io.izzel.arclight.common.bridge.core.world.damagesource.CombatEntryBridge;
import io.izzel.arclight.common.bridge.core.world.damagesource.CombatTrackerBridge;
import io.papermc.paper.world.damagesource.PaperCombatTrackerWrapper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.damagesource.CombatEntry;
import net.minecraft.world.damagesource.CombatTracker;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(CombatTracker.class)
public abstract class CombatTrackerMixin implements CombatTrackerBridge {

    @Shadow @Final public List<CombatEntry> entries;
    @Shadow @Final public LivingEntity mob;
    @Shadow private int lastDamageTime;
    @Shadow private int combatStartTime;
    @Shadow private int combatEndTime;
    @Shadow public boolean inCombat;
    @Shadow public boolean takingDamage;

    @Shadow
    private static boolean shouldEnterCombat(DamageSource source) {
        throw new AssertionError();
    }

    @Override
    @Invoker("getMostSignificantFall")
    public abstract @Nullable CombatEntry bridge$getMostSignificantFall();

    @Unique
    private PaperCombatTrackerWrapper arclight$paperCombatTracker;

    @Unique
    private Component arclight$emptyComnent;

    @Inject(method = "getDeathMessage", cancellable = true, at = @At("HEAD"))
    private void arclight$useOverride(CallbackInfoReturnable<Component> cir) {
        if (!this.entries.isEmpty()) {
            var entry = this.entries.get(this.entries.size() - 1);
            var deathMessage = ((CombatEntryBridge) (Object) entry).bridge$getDeathMessage();
            if (deathMessage != null) {
                cir.setReturnValue(deathMessage);
            }
        } else {
            if (this.arclight$emptyComnent != null) {
                cir.setReturnValue(this.arclight$emptyComnent);
            }
        }
        this.arclight$emptyComnent = null;
    }

    @Override
    public void bridge$setDeathMessage(Component component) {
        this.arclight$emptyComnent = component;
        if (!this.entries.isEmpty()) {
            var entry = this.entries.getLast();
            ((CombatEntryBridge) (Object) entry).bridge$setDeathMessage(component);
        }
    }

    @Override
    public PaperCombatTrackerWrapper bridge$getPaperCombatTracker() {
        if (this.arclight$paperCombatTracker == null) {
            this.arclight$paperCombatTracker = new PaperCombatTrackerWrapper((CombatTracker) (Object) this);
        }
        return this.arclight$paperCombatTracker;
    }

    @Override
    public List<CombatEntry> bridge$getEntries() {
        return this.entries;
    }

    @Override
    public LivingEntity bridge$getMob() {
        return this.mob;
    }

    @Override
    public boolean bridge$isInCombat() {
        return this.inCombat;
    }

    @Override
    public boolean bridge$isTakingDamage() {
        return this.takingDamage;
    }

    @Override
    public void bridge$recordDamageAndCheckCombatState(CombatEntry entry) {
        // Native recordDamage tail: keep this entry's supplied fall attribution intact.
        this.entries.add(entry);
        this.lastDamageTime = this.mob.tickCount;
        this.takingDamage = true;
        if (!this.inCombat && this.mob.isAlive() && shouldEnterCombat(entry.source())) {
            this.inCombat = true;
            this.combatStartTime = this.mob.tickCount;
            this.combatEndTime = this.combatStartTime;
            this.mob.onEnterCombat();
        }
    }

    @Override
    public void bridge$resetCombatState() {
        // Native recheckStatus reset tail, without its timeout/takingDamage guard.
        boolean wasInCombat = this.inCombat;
        this.takingDamage = false;
        this.inCombat = false;
        this.combatEndTime = this.mob.tickCount;
        if (wasInCombat) {
            this.mob.onLeaveCombat();
        }
        this.entries.clear();
    }

    public void recordDamageAndCheckCombatState(net.minecraft.world.damagesource.CombatEntry entry) {
        this.bridge$recordDamageAndCheckCombatState(entry);
    }

    public void resetCombatState() {
        this.bridge$resetCombatState();
    }

}
