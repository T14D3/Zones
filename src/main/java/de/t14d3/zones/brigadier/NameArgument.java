package de.t14d3.zones.brigadier;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import io.papermc.paper.command.brigadier.argument.CustomArgumentType;

public class NameArgument implements CustomArgumentType.Converted<String, String> {
    @Override
    public String convert(String arg) {
        return arg;
    }

    @Override
    public ArgumentType<String> getNativeType() {
        return StringArgumentType.word();
    }
}
