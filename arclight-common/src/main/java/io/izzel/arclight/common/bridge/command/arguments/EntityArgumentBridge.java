package io.izzel.arclight.common.bridge.command.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.arguments.EntitySelector;

public interface EntityArgumentBridge {

    EntitySelector bridge$parse(StringReader reader, boolean overridePermissions) throws CommandSyntaxException;
}
