package de.t14d3.zones;

import de.t14d3.zones.permissions.PermissionManager;
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

    @Override
    public void onInitializeServer() {
        ServerLifecycleEvents.SERVER_STARTING.register(this::onEnable);
        this.platform = new FabricPlatform(this);

        this.types = new FabricTypes();
        this.dataFolder = FabricLoader.getInstance().getConfigDir().toFile();

        Zones.getInstance().getLogger().info("Zones Fabric mod initialized!");
    }

    private void onEnable(@NotNull MinecraftServer server) {
        this.server = server;
        this.playerManager = server.getPlayerManager();
    }

    public File getDataFolder() {
        return dataFolder;
    }

    public PermissionManager getPermissionManager() {
        return null;
    }

    public Types getTypes() {
        return types;
    }

    public MinecraftServer getServer() {
        return server;
    }
}
