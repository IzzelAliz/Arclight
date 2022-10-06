package io.izzel.arclight.common.mixin.core.network.protocol.game;

import it.unimi.dsi.fastutil.shorts.ShortSet;
import net.minecraft.core.SectionPos;
import net.minecraft.network.protocol.game.ClientboundSectionBlocksUpdatePacket;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ClientboundSectionBlocksUpdatePacket.class)
public class ClientboundSectionBlocksUpdatePacketMixin {

    @Shadow @Final @Mutable private SectionPos sectionPos;
    @Shadow @Final @Mutable private boolean suppressLightUpdates;
    @Shadow @Final @Mutable private short[] positions;
    @Shadow @Final @Mutable private BlockState[] states;

    public void arclight$constructor(SectionPos sectionposition, ShortSet shortset, BlockState[] states, boolean flag) {
        this.sectionPos = sectionposition;
        this.suppressLightUpdates = flag;
        this.positions = shortset.toShortArray();
        this.states = states;
    }
}
