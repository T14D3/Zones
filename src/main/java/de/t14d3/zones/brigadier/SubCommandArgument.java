package de.t14d3.zones.brigadier;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import io.papermc.paper.command.brigadier.MessageComponentSerializer;
import io.papermc.paper.command.brigadier.argument.CustomArgumentType;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;


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

    @Override
    public <S> @NotNull CompletableFuture<Suggestions> listSuggestions(@NotNull CommandContext<S> context, @NotNull SuggestionsBuilder builder) {
        for (SubCommands subCommand : SubCommands.values()) {
            builder.suggest(subCommand.name(), MessageComponentSerializer.message().serialize(subCommand.getInfo()));
        }
        return builder.buildFuture();
    }
}
