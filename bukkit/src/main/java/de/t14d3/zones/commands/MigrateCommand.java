package de.t14d3.zones.commands;

import de.t14d3.zones.Region;
import de.t14d3.zones.Zones;
import de.t14d3.zones.datasource.AbstractDataSource;
import de.t14d3.zones.datasource.DataSourceManager;
import de.t14d3.zones.datasource.SQLDataSource;
import de.t14d3.zones.datasource.YamlDataSource;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.StringArgument;

import java.util.List;
import java.util.stream.Stream;

public class MigrateCommand {
    private Zones plugin;

    public MigrateCommand(Zones plugin) {
        this.plugin = plugin;
    }

    public CommandAPICommand migrate = new CommandAPICommand("migrate")
            .withPermission("zones.migrate")
            .withArguments(
                    new StringArgument("targetType")
                            .replaceSuggestions(ArgumentSuggestions.strings(info ->
                                    Stream.of(DataSourceManager.DataSourceTypes.values()).map(
                                            DataSourceManager.DataSourceTypes::name).toArray(String[]::new))))
            .executes((sender, args) -> {
                DataSourceManager.DataSourceTypes targetType = DataSourceManager.DataSourceTypes.valueOf(
                        args.getRaw("targetType").toUpperCase());
                DataSourceManager dataSourceManager = plugin.getRegionManager().getDataSourceManager();

                // Load current regions from the existing datasource
                List<Region> regions = dataSourceManager.loadRegions();

                // Initialize target datasource
                AbstractDataSource targetDataSource;
                switch (targetType) {
                    case YAML:
                        targetDataSource = new YamlDataSource(plugin.getDataFolder(), plugin);
                        break;
                    case SQLITE, MYSQL, H2, POSTGRESQL, CUSTOM:
                        targetDataSource = new SQLDataSource(plugin, targetType);
                        break;
                    default:
                        sender.sendMessage("Invalid target datasource type.");
                        return;
                }
                // Save regions to the new datasource
                targetDataSource.saveRegions(regions);

                sender.sendMessage("Migration to " + targetType + " completed successfully.");
            });
}
