package io.izzel.arclight.common.bridge.core.command.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.arguments.selector.EntitySelector;

public interface EntityArgumentBridge {

    EntitySelector bridge$parse(StringReader reader, boolean overridePermissions) throws CommandSyntaxException;
}
