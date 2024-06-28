package io.izzel.arclight.common.mixin.core.world.entity.decoration;

import io.izzel.arclight.common.mixin.core.world.entity.item.BlockAttachedEntityMixin;
import net.minecraft.network.protocol.game.ClientboundSetEntityLinkPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Leashable;
import net.minecraft.world.entity.decoration.LeashFenceKnotEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.LeadItem;
import net.minecraft.world.level.gameevent.GameEvent;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.bukkit.event.entity.EntityRemoveEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import java.util.List;

@Mixin(LeashFenceKnotEntity.class)
public abstract class LeashFenceKnotEntityMixin extends BlockAttachedEntityMixin {

    /**
     * @author IzzelAliz
     * @reason
     */
    @SuppressWarnings("ConstantConditions")
    @Overwrite
    public InteractionResult interact(final Player entityhuman, final InteractionHand enumhand) {
        if (this.level().isClientSide) {
            return InteractionResult.SUCCESS;
        }
        boolean flag = false;
        List<Leashable> list = LeadItem.leashableInArea(this.level(), this.getPos(), (leashable) -> {
            Entity entity = leashable.getLeashHolder();

            return entity == entityhuman || entity == (Object) this;
        });
        for (var leashable : list) {
            if (leashable.getLeashHolder() == entityhuman) {
                if (leashable instanceof Entity entity) {
                    if (CraftEventFactory.callPlayerLeashEntityEvent(entity, (LeashFenceKnotEntity) (Object) this, entityhuman, enumhand).isCancelled()) {
                        ((ServerPlayer) entityhuman).connection.send(new ClientboundSetEntityLinkPacket(entity, leashable.getLeashHolder()));
                        flag = true;
                        continue;
                    }
                }
                leashable.setLeashedTo((LeashFenceKnotEntity) (Object) this, true);
                flag = true;
            }
        }
        boolean flag1 = false;
        if (!flag) {
            boolean die = true;
            for (var leashable : list) {
                if (leashable.isLeashed() && leashable.getLeashHolder() == (Object) this) {
                    if (leashable instanceof Entity entity) {
                        if (CraftEventFactory.callPlayerUnleashEntityEvent(entity, entityhuman, enumhand).isCancelled()) {
                            die = false;
                            continue;
                        }
                    }
                    leashable.dropLeash(true, !entityhuman.getAbilities().instabuild);
                    flag1 = true;
                }
            }
            if (die) {
                this.bridge$pushEntityRemoveCause(EntityRemoveEvent.Cause.DROP);
                this.discard();
            }
        }
        if (flag || flag1) {
            this.gameEvent(GameEvent.BLOCK_ATTACH, entityhuman);
        }
        return InteractionResult.CONSUME;
    }
}
