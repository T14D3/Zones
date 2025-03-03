package de.t14d3.zones.fabric.commands;

import com.mojang.brigadier.context.CommandContext;
import de.t14d3.zones.Region;
import de.t14d3.zones.datasource.AbstractDataSource;
import de.t14d3.zones.datasource.DataSourceManager;
import de.t14d3.zones.datasource.SQLDataSource;
import de.t14d3.zones.datasource.YamlDataSource;
import de.t14d3.zones.fabric.ZonesFabric;
import net.kyori.adventure.text.Component;
import net.minecraft.commands.CommandSourceStack;

import java.util.List;

public class MigrateCommand {
    private final ZonesFabric mod;

    public MigrateCommand(ZonesFabric mod) {
        this.mod = mod;
    }

    int execute(CommandContext<CommandSourceStack> context) {
        DataSourceManager.DataSourceTypes targetType = DataSourceManager.DataSourceTypes.valueOf(
                context.getArgument("type", String.class).toUpperCase());

        DataSourceManager dataSourceManager = mod.getRegionManager().getDataSourceManager();

        // Load current regions from the existing datasource
        List<Region> regions = dataSourceManager.loadRegions();

        // Initialize target datasource
        AbstractDataSource targetDataSource;
        switch (targetType) {
            case YAML:
                targetDataSource = new YamlDataSource(mod.getDataFolder(), mod.getZones());
                break;
            case SQLITE, MYSQL, H2, POSTGRESQL, CUSTOM:
                targetDataSource = new SQLDataSource(mod.getZones(), targetType);
                break;
            default:
                context.getSource().sendMessage(Component.text("Invalid target datasource type."));
                return 1;
        }
        // Save regions to the new datasource
        targetDataSource.saveRegions(regions);
        context.getSource().sendMessage(Component.text("Migration to " + targetType + " completed successfully."));
        return 1;
    }
}
