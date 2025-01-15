package de.t14d3.zones.brigadier;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.papermc.paper.command.brigadier.argument.CustomArgumentType;
import org.jetbrains.annotations.NotNull;


@SuppressWarnings("UnstableApiUsage")
public class SubCommandArgument implements CustomArgumentType.Converted<SubCommands, String> {
    @Override
    public @NotNull SubCommands convert(@NotNull String nativeType) throws CommandSyntaxException {
        return SubCommands.get(nativeType.toUpperCase());
    }

    @Override
    public @NotNull ArgumentType<String> getNativeType() {
        return StringArgumentType.string();
    }
}
