package io.izzel.arclight.common.mixin.core.world.level.dimension.end;

import io.izzel.arclight.common.bridge.core.world.level.dimension.end.EnderDragonFightBridge;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import io.izzel.arclight.mixin.Local;
import io.papermc.paper.event.block.DragonEggFormEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.dimension.end.DragonRespawnStage;
import net.minecraft.world.level.dimension.end.EnderDragonFight;
import net.minecraft.world.level.saveddata.SavedData;
import org.bukkit.craftbukkit.block.CraftBlock;
import org.bukkit.craftbukkit.block.CraftBlockState;
import org.bukkit.craftbukkit.block.CraftBlockStates;
import org.bukkit.craftbukkit.boss.CraftDragonBattle;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;

/** Preserve the native void respawn paths and derive Bukkit's result from the transition. */
@Mixin(EnderDragonFight.class)
public abstract class EnderDragonFightMixin extends SavedData implements EnderDragonFightBridge {

    @Shadow public DragonRespawnStage respawnStage;
    @Shadow private boolean dragonKilled;
    @Shadow public boolean hasPreviouslyKilledDragon;
    @Shadow public abstract void tryRespawn();

    @Invoker("respawnDragon")
    public abstract void arclight$invokeRespawnDragon(List<EndCrystal> crystals);

    @Override
    public boolean bridge$tryRespawn() {
        // Keep the native portal/crystal search; do not count an already-active fight as success.
        boolean eligible = this.dragonKilled && this.respawnStage == null;
        this.tryRespawn();
        return eligible && this.respawnStage == DragonRespawnStage.START;
    }

    @Override
    public boolean bridge$respawnDragon(List<EndCrystal> crystals) {
        boolean eligible = this.dragonKilled && this.respawnStage == null;
        this.arclight$invokeRespawnDragon(crystals);
        return eligible && this.respawnStage == DragonRespawnStage.START;
    }

    @Decorate(method = "setDragonKilled(Lnet/minecraft/world/entity/boss/enderdragon/EnderDragon;)V",
        at = @At(value = "FIELD", opcode = Opcodes.GETFIELD,
            target = "Lnet/minecraft/world/level/dimension/end/EnderDragonFight;hasPreviouslyKilledDragon:Z"),
        require = 1, allow = 1)
    private boolean arclight$includeDragonEggAttempt(EnderDragonFight fight,
            @Local(allocate = "arclightPreviouslyKilledDragon") boolean previouslyKilled) throws Throwable {
        previouslyKilled = (boolean) DecorationOps.callsite().invoke(fight);
        DecorationOps.blackhole().invoke(previouslyKilled);
        // Paper dispatches on every tracked dragon death, including the normally cancelled repeat attempt.
        // Change only this branch operand, not the fight's persisted previous-kill state.
        return false;
    }

    @Decorate(method = "setDragonKilled(Lnet/minecraft/world/entity/boss/enderdragon/EnderDragon;)V",
        at = @At(value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerLevel;setBlockAndUpdate(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Z"),
        require = 1, allow = 1)
    private boolean arclight$dragonEggForm(ServerLevel level, BlockPos pos, BlockState proposed,
            @Local(allocate = "arclightPreviouslyKilledDragon") boolean previouslyKilled) throws Throwable {
        CraftBlockState state = CraftBlockStates.getBlockState(level, pos);
        state.setBlock(proposed);
        DragonEggFormEvent event = new DragonEggFormEvent(CraftBlock.at(level, pos), state,
            new CraftDragonBattle((EnderDragonFight) (Object) this));
        event.setCancelled(previouslyKilled && !((io.izzel.arclight.common.bridge.core.world.level.WorldBridge) level)
            .bridge$paperConfig().entities.behavior.enderDragonsDeathAlwaysPlacesDragonEgg);
        if (!event.callEvent()) {
            return false;
        }
        // Native setBlockAndUpdate uses UPDATE_ALL, as Paper's CraftBlockState.place does.
        return (boolean) DecorationOps.callsite().invoke(level, pos, ((CraftBlockState) event.getNewState()).getHandle());
    }

    @Decorate(method = "onCrystalDestroyed(Lnet/minecraft/world/entity/boss/enderdragon/EndCrystal;Lnet/minecraft/world/damagesource/DamageSource;)V",
        at = @At(value = "INVOKE", target = "Ljava/util/List;contains(Ljava/lang/Object;)Z"))
    private boolean arclight$matchesRespawnCrystal(List<EntityReference<EndCrystal>> references, Object candidate) throws Throwable {
        boolean contained = (boolean) DecorationOps.callsite().invoke(references, candidate);
        // Native stores EntityReference values, while the destruction callback passes an EndCrystal.
        if (contained || !(candidate instanceof EndCrystal crystal)) {
            return contained;
        }
        return references.stream().anyMatch(reference -> reference.matches(crystal));
    }

    @Override
    public void bridge$setPreviouslyKilled(boolean killed) {
        if (this.hasPreviouslyKilledDragon != killed) {
            this.hasPreviouslyKilledDragon = killed;
            this.setDirty();
        }
    }
}
