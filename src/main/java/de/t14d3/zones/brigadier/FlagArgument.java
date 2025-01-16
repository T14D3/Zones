package de.t14d3.zones.brigadier;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import de.t14d3.zones.utils.Actions;
import io.papermc.paper.command.brigadier.argument.CustomArgumentType;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("UnstableApiUsage")
public class FlagArgument implements CustomArgumentType.Converted<Actions, String> {

    @Override
    public @NotNull Actions convert(String nativeType) throws CommandSyntaxException {
        return Actions.valueOf(nativeType.toUpperCase());
    }

    @Override
    public @NotNull ArgumentType<String> getNativeType() {
        return StringArgumentType.word();
    }
}
