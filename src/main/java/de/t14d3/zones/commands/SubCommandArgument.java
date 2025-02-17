package de.t14d3.zones.commands;

import com.mojang.brigadier.Message;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import de.t14d3.zones.Zones;
import io.papermc.paper.command.brigadier.MessageComponentSerializer;
import io.papermc.paper.command.brigadier.argument.CustomArgumentType;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.jetbrains.annotations.NotNull;


@SuppressWarnings("UnstableApiUsage")
public class SubCommandArgument implements CustomArgumentType.Converted<SubCommands, String> {
    @Override
    public @NotNull SubCommands convert(@NotNull String nativeType) throws CommandSyntaxException {
        try {
            return SubCommands.valueOf(nativeType.toUpperCase().split(" ")[0]);
        } catch (IllegalArgumentException ignored) {
            Message message = MessageComponentSerializer.message().serialize(
                    MiniMessage.miniMessage().deserialize(Zones.getInstance().getMessages().get("commands.invalid")));
            throw new CommandSyntaxException(new SimpleCommandExceptionType(message), message);
        }
    }

    @Override
    public @NotNull ArgumentType<String> getNativeType() {
        return StringArgumentType.string();
    }
}
