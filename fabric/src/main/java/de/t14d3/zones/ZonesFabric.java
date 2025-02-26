package de.t14d3.zones;

import de.t14d3.zones.fabric.commands.RootCommand;
import de.t14d3.zones.fabric.listeners.PlayerListener;
import de.t14d3.zones.utils.Messages;
import de.t14d3.zones.utils.Types;
import me.lucko.fabric.api.permissions.v0.PermissionCheckEvent;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.util.TriState;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.concurrent.atomic.AtomicReference;

public class ZonesFabric implements DedicatedServerModInitializer {
    private ZonesPlatform platform;
    private FabricTypes types;
    private PlayerManager playerManager;
    private File dataFolder;
    private MinecraftServer server;
    private Zones zones;
    private FabricPermissionManager permissionManager;
    private PlayerListener playerListener;

    private RegionManager regionManager;
    private Messages messages;
    private RootCommand rootCommand;

    @Override
    public void onInitializeServer() {
        ServerLifecycleEvents.SERVER_STARTING.register(this::onEnable);
        ServerLifecycleEvents.SERVER_STARTED.register(this::onStarted);
        ServerLifecycleEvents.SERVER_STOPPING.register(this::onDisable);

        this.dataFolder = new File(FabricLoader.getInstance().getConfigDir().toFile(), "Zones");
        this.platform = new FabricPlatform(this);

        this.types = new FabricTypes(this);

        this.zones = new Zones(platform);
        this.regionManager = zones.getRegionManager();
        this.messages = zones.getMessages();

        this.permissionManager = new FabricPermissionManager(zones);

        this.rootCommand = new RootCommand(this);

        Zones.getInstance().getLogger().info("Zones Fabric mod initialized!");
    }

    private void onEnable(@NotNull MinecraftServer server) {
        this.server = server;
        this.playerManager = server.getPlayerManager();
        this.playerListener = new PlayerListener(this);

        // If luckperms is not loaded, register our own scuffed permission system,
        // otherwise skip to have luckperms handle permissions
        if (!FabricLoader.getInstance().isModLoaded("luckperms")) {
            PermissionCheckEvent.EVENT.register((source, permission) -> {
                if (!permission.startsWith("zones.")) {
                    return TriState.DEFAULT;
                }
                AtomicReference<TriState> result = new AtomicReference<>(TriState.DEFAULT);
                permissionManager.getPermissions().stream().filter(p -> p.getName().equals(permission)).findFirst()
                        .ifPresent(p -> {
                            if (source.hasPermissionLevel(p.getLevel())) {
                                result.set(TriState.TRUE);
                            }
                        });
                return result.get();
            });
        }
    }

    private void onStarted(MinecraftServer server) {
        ((FabricPlatform) zones.getPlatform()).loadWorlds(server);

        regionManager.loadRegions();
        zones.getLogger().info("Mod enabled, loaded {} regions.", zones.getRegionManager().regions().size());
    }

    private void onDisable(MinecraftServer server) {
        regionManager.saveRegions();
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

    public FabricPlatform getPlatform() {
        return (FabricPlatform) platform;
    }
}
