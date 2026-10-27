package io.izzel.arclight.neoforge.mixin.neoforge;

import io.izzel.arclight.common.mod.server.event.ArclightEventFactory.BlockDestruction;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import net.minecraft.world.level.gamerules.GameRules;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(CommonHooks.class)
public abstract class CommonHooksBlockDestructionEventMixin_NeoForge {

    @Decorate(method = "handleBlockDrops", remap = false,
        at = @At(value = "INVOKE", target = "Lnet/neoforged/bus/api/IEventBus;post(Lnet/neoforged/bus/api/Event;)Lnet/neoforged/bus/api/Event;", remap = false), require = 1, allow = 1)
    private static Event arclight$paperBlockDestruction(IEventBus bus, Event event) throws Throwable {
        if (event instanceof BlockDropsEvent drops) {
            BlockDestruction context = BlockDestruction.current(drops.getLevel(), drops.getPos());
            if (context != null) {
                if (context.isBlockBreak()) {
                    context.editNeoForgeDrops(drops.getLevel(), drops.getPos(), drops.getDrops(), drops.getDroppedExperience(),
                        () -> drops.getLevel().getGameRules().get(GameRules.BLOCK_DROPS) && !drops.getLevel().restoringBlockSnapshots);
                }
                // Seed the existing loader event, not a second out-of-band XP emission.
                // Neo listeners retain their native right to edit XP/items or cancel both.
                drops.setDroppedExperience(Math.max(0, context.experience()));
            }
        }
        return (Event) DecorationOps.callsite().invoke(bus, event);
    }
}
