package io.izzel.arclight.common.mixin.core.command.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.izzel.arclight.common.bridge.command.arguments.EntityArgumentBridge;
import io.izzel.arclight.common.bridge.command.arguments.EntitySelectorParserBridge;
import net.minecraft.command.arguments.EntityArgument;
import net.minecraft.command.arguments.EntitySelector;
import net.minecraft.command.arguments.EntitySelectorParser;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import static net.minecraft.command.arguments.EntityArgument.ONLY_PLAYERS_ALLOWED;
import static net.minecraft.command.arguments.EntityArgument.TOO_MANY_ENTITIES;
import static net.minecraft.command.arguments.EntityArgument.TOO_MANY_PLAYERS;

@Mixin(EntityArgument.class)
public class EntityArgumentMixin implements EntityArgumentBridge {

    // @formatter:off
    @Shadow @Final private boolean single;
    @Shadow @Final private boolean playersOnly;
    // @formatter:on

    @Override
    public EntitySelector bridge$parse(StringReader reader, boolean overridePermissions) throws CommandSyntaxException {
        return this.parse(reader, overridePermissions);
    }

    public EntitySelector parse(StringReader reader, boolean overridePermissions) throws CommandSyntaxException {
        int i = 0;
        EntitySelectorParser entityselectorparser = new EntitySelectorParser(reader);
        EntitySelector entityselector = ((EntitySelectorParserBridge) entityselectorparser).bridge$parse(overridePermissions);
        if (entityselector.getLimit() > 1 && this.single) {
            if (this.playersOnly) {
                reader.setCursor(0);
                throw TOO_MANY_PLAYERS.createWithContext(reader);
            } else {
                reader.setCursor(0);
                throw TOO_MANY_ENTITIES.createWithContext(reader);
            }
        } else if (entityselector.includesEntities() && this.playersOnly && !entityselector.isSelfSelector()) {
            reader.setCursor(0);
            throw ONLY_PLAYERS_ALLOWED.createWithContext(reader);
        } else {
            return entityselector;
        }
    }
}
