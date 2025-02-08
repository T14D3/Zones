package de.t14d3.zones.commands;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import de.t14d3.zones.PaperBootstrap;
import de.t14d3.zones.permissions.Flags;
import io.papermc.paper.command.brigadier.argument.CustomArgumentType;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("UnstableApiUsage")
public class FlagArgument implements CustomArgumentType.Converted<String, String> {

    public FlagArgument(PaperBootstrap context) {
    }

    @Override
    public String convert(@NotNull String nativeType) throws CommandSyntaxException {
        return Flags.getFlag(nativeType).name();
    }

    @Override
    public @NotNull ArgumentType<String> getNativeType() {
        return StringArgumentType.word();
    }
}
