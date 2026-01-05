package de.t14d3.zones.fabric;

import de.t14d3.rapunzellib.Rapunzel;
import de.t14d3.rapunzellib.events.GameEvents;
import de.t14d3.rapunzellib.message.MessageFormatService;
import de.t14d3.rapunzellib.platform.fabric.FabricRapunzelBootstrap;
import de.t14d3.zones.RegionManager;
import de.t14d3.zones.Zones;
import de.t14d3.zones.ZonesPlatform;
import de.t14d3.zones.fabric.commands.RootCommand;
import de.t14d3.zones.rapunzellib.ZonesRapunzelHooks;
import de.t14d3.zones.utils.Types;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import org.jetbrains.annotations.NotNull;

import java.io.File;

/**
 * Fabric entrypoint for Zones.
 *
 * <p>Initializes Zones on dedicated servers and wires Fabric lifecycle callbacks into RapunzelLib.</p>
 */
public class ZonesFabric implements DedicatedServerModInitializer {
    private static final String MOD_ID = "zones";

    private ZonesPlatform platform;
    private FabricTypes types;
    private File dataFolder;
    private MinecraftServer server;
    private Zones zones;
    private FabricPermissionManager permissionManager;

    private RegionManager regionManager;
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

        this.permissionManager = (FabricPermissionManager) zones.getPermissionManager();

        Zones.getInstance().getLogger().info("Zones Fabric mod initialized!");
    }

    void onEnable(@NotNull MinecraftServer server) {
        this.server = server;

        FabricRapunzelBootstrap.bootstrap(MOD_ID, server, ZonesFabric.class);
        GameEvents.install(this);
        ZonesRapunzelHooks.install(zones);
        this.rootCommand = new RootCommand(this);
        // Fabric callbacks are bridged into RapunzelLib GameEvents; Zones subscribes via ZonesRapunzelHooks.
    }

    void onStarted(MinecraftServer server) {
        regionManager.loadRegions();
        zones.getLogger().info("Mod enabled, loaded {} regions.", zones.getRegionManager().regions().size());

        String primaryType = zones.getConfig().getString("visuals.particles.primary", "WAX_OFF").toLowerCase();
        String secondaryType = zones.getConfig().getString("visuals.particles.secondary", "WAX_ON").toLowerCase();
        String previewType = zones.getConfig().getString("visuals.particles.preview", "COMPOSTER").toLowerCase();
        ParticleType<?> primary = BuiltInRegistries.PARTICLE_TYPE
                .get(ResourceLocation.withDefaultNamespace(primaryType))        
                .map(ref -> ref.value())
                .orElseThrow();
        ParticleType<?> secondary = BuiltInRegistries.PARTICLE_TYPE
                .get(ResourceLocation.withDefaultNamespace(secondaryType))
                .map(ref -> ref.value())
                .orElseThrow();
        ParticleType<?> preview = BuiltInRegistries.PARTICLE_TYPE
                .get(ResourceLocation.withDefaultNamespace(previewType))
                .map(ref -> ref.value())
                .orElseThrow();
        ((FabricPlatform) platform).primary = (SimpleParticleType) primary;
        ((FabricPlatform) platform).secondary = (SimpleParticleType) secondary;
        ((FabricPlatform) platform).preview = (SimpleParticleType) preview;

    }

    void onDisable(MinecraftServer server) {
        regionManager.saveRegions();
        Rapunzel.shutdown(MOD_ID);
    }

    /**
     * Returns the mod's data folder (config directory).
     */
    public File getDataFolder() {
        return dataFolder;
    }

    /**
     * Returns the permission manager implementation for Fabric.
     */
    public FabricPermissionManager getPermissionManager() {
        return this.permissionManager;
    }

    /**
     * Returns the platform types registry for Fabric.
     */
    public Types getTypes() {
        return types;
    }

    /**
     * Returns the Minecraft server instance (available after SERVER_STARTING).
     */
    public MinecraftServer getServer() {
        return server;
    }

    /**
     * Returns the shared region manager.
     */
    public RegionManager getRegionManager() {
        return regionManager;
    }

    /**
     * Returns the message formatter service used by Zones.
     */
    public MessageFormatService getMessages() {
        return zones.getMessages();
    }

    /**
     * Returns the core Zones instance.
     */
    public Zones getZones() {
        return zones;
    }

    /**
     * Returns the Fabric platform bridge.
     */
    public FabricPlatform getPlatform() {
        return (FabricPlatform) platform;
    }
}
