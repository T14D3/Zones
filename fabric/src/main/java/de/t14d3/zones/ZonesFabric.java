package de.t14d3.zones;

import de.t14d3.zones.fabric.commands.CancelCommand;
import de.t14d3.zones.fabric.commands.CreateCommand;
import de.t14d3.zones.fabric.commands.DeleteCommand;
import de.t14d3.zones.fabric.listeners.PlayerListener;
import de.t14d3.zones.utils.Messages;
import de.t14d3.zones.utils.Types;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import org.jetbrains.annotations.NotNull;

import java.io.File;

public class ZonesFabric implements DedicatedServerModInitializer {
    private ZonesPlatform platform;
    private FabricTypes types;
    private PlayerManager playerManager;
    private File dataFolder;
    private MinecraftServer server;
    private Zones zones;
    private FabricPermissionManager permissionManager;

    private RegionManager regionManager;
    private Messages messages;

    private CancelCommand cancelCommand;
    private CreateCommand createCommand;
    private DeleteCommand deleteCommand;

    @Override
    public void onInitializeServer() {
        ServerLifecycleEvents.SERVER_STARTING.register(this::onEnable);
        this.platform = new FabricPlatform(this);

        this.types = new FabricTypes();
        this.dataFolder = FabricLoader.getInstance().getConfigDir().toFile();

        this.zones = new Zones(platform);
        this.regionManager = zones.getRegionManager();
        this.messages = zones.getMessages();

        this.permissionManager = new FabricPermissionManager(zones);

        this.cancelCommand = new CancelCommand(this);
        this.createCommand = new CreateCommand(this);
        this.deleteCommand = new DeleteCommand(this);
        this.cancelCommand.register();
        this.createCommand.register();
        this.deleteCommand.register();

        Zones.getInstance().getLogger().info("Zones Fabric mod initialized!");
    }

    private void onEnable(@NotNull MinecraftServer server) {
        this.server = server;
        this.playerManager = server.getPlayerManager();
        new PlayerListener(this);
    }

    public File getDataFolder() {
        return dataFolder;
    }

    public FabricPermissionManager getPermissionManager() {
        return this.permissionManager;
    }

    public Types getTypes() {
        return types;
    }

    public MinecraftServer getServer() {
        return server;
    }

    public RegionManager getRegionManager() {
        return regionManager;
    }

    public Messages getMessages() {
        return messages;
    }

    public Zones getZones() {
        return zones;
    }
}
