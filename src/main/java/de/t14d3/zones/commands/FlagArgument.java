package de.t14d3.zones.commands;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import de.t14d3.zones.PaperBootstrap;
import de.t14d3.zones.utils.Flags;
import io.papermc.paper.command.brigadier.argument.CustomArgumentType;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("UnstableApiUsage")
public class FlagArgument implements CustomArgumentType.Converted<String, String> {
    private final Flags flags;

    public FlagArgument(PaperBootstrap context) {
        this.flags = context.getFlags();
    }
    @Override
    public @NotNull String convert(@NotNull String nativeType) throws CommandSyntaxException {
        return flags.getFlags().get(nativeType);
    }

    @Override
    public @NotNull ArgumentType<String> getNativeType() {
        return StringArgumentType.word();
    }
}
