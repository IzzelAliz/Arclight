package io.izzel.arclight.common.mixin.core.world.entity.animal;

import io.izzel.arclight.common.mod.util.FertilizeEggRandom;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import io.izzel.arclight.mixin.Local;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.animal.Animal;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(targets = "net.minecraft.world.entity.animal.turtle.Turtle$TurtleBreedGoal")
public abstract class TurtleFertilizeEggEventMixin extends BreedGoal {

    public TurtleFertilizeEggEventMixin(Animal animal, double speedModifier) {
        super(animal, speedModifier);
    }

    @Decorate(method = "breed()V", inject = true, at = @At(value = "JUMP", opcode = Opcodes.IFNULL, ordinal = 1),
        require = 1, allow = 1)
    private void arclight$fertilizeEgg(@Local(allocate = "fertilizeEggExperience") int experience) throws Throwable {
        // Both native loaders have selected loveCause here, before stats or HasEgg changes.
        var event = CraftEventFactory.callEntityFertilizeEggEvent(this.animal, this.partner);
        if (event.isCancelled()) {
            DecorationOps.cancel().invoke();
            return;
        }
        experience = event.getExperience();
        DecorationOps.blackhole().invoke(experience);
    }

    @Decorate(method = "breed()V", inject = true,
        at = @At(value = "NEW", target = "(Lnet/minecraft/world/level/Level;DDDI)Lnet/minecraft/world/entity/ExperienceOrb;"),
        require = 1, allow = 1)
    private void arclight$skipFertilizeEggExperience(@Local(allocate = "fertilizeEggExperience") int experience) throws Throwable {
        if (experience <= 0) {
            // This allocation/add is the final native action; retain all preceding breeding state.
            DecorationOps.cancel().invoke();
            return;
        }
    }

    @Decorate(method = "breed()V", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/RandomSource;nextInt(I)I"),
        require = 1, allow = 1)
    private int arclight$fertilizeEggExperience(RandomSource random, int bound,
                                               @Local(allocate = "fertilizeEggExperience") int experience) throws Throwable {
        // The helper already made the XP roll. Adapt only this receiver, keeping the current
        // (possibly mod-wrapped) callsite and the native +1/constructor/add path intact.
        RandomSource experienceRandom = new FertilizeEggRandom(random, experience);
        return (int) DecorationOps.callsite().invoke(experienceRandom, bound);
    }
}
