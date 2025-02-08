package de.t14d3.zones.utils;

import de.t14d3.zones.Zones;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

public final class DebugLoggerManager {
    private final Zones plugin;
    private final DebugLoggerInterface logger;

    public static final String CACHE_HIT = "Cache hit on action: ";
    public static final String CACHE_MISS = "Cache miss on action: ";
    public static final String UNI_CHECK = "UNIVERSAL Check";
    public static final String CHECK = "Checking action: ";
    public static final String PERM = "Checking permission: ";
    public static final String PARENT = "Checking parent for: ";
    public static final String RESULT = "Calculated result: ";


    public DebugLoggerManager(Zones plugin) {
        this.plugin = plugin;
        boolean debugEnabled = plugin.getConfig().getBoolean("debug", false)
                || Objects.equals(System.getenv("ZONES_DEBUG"), "true");
        this.logger = debugEnabled ? new DebugLogger() : new DebugLoggerDummy();
    }

    public void log(Object... objects) {
        logger.log(objects);
    }

    public void log(String message, Object context) {
        logger.log(message, context);
    }

    private interface DebugLoggerInterface {
        void log(Object... objects);
    }

    private static final class DebugLoggerDummy implements DebugLoggerInterface {
        @Override
        public void log(Object... objects) {
            // Intentionally empty: Skip logging string construction in non-debug mode
        }
    }

    private final class DebugLogger implements DebugLoggerInterface {
        private final Logger logger;

        public DebugLogger() {
            this.logger = LoggerFactory.getLogger(plugin.getClass());
            logger.error("///////////////////////////////////////////////////////////////////////////////////");
            logger.error("| Zones Debug Mode Enabled                                                        |");
            logger.error("| This mode is only for development purposes and causes severe performance issues.|");
            logger.error("| Please disable this mode in production environments.                            |");
            logger.error("///////////////////////////////////////////////////////////////////////////////////");
        }

        @Override
        public void log(Object... objects) {
            StringBuilder builder = new StringBuilder();
            for (Object object : objects) {
                builder.append(object.toString()).append(" ");
            }
            this.logger.info("[DEBUG]: {}", builder.toString().trim());
        }
    }
}