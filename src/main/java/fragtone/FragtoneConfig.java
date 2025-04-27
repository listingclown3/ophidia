package fragtone;

import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

import java.io.File;

public class FragtoneConfig {
    private static Configuration config;

    // WebSocket Settings
    private static String websocketUrl = "ws://localhost:8080";
    private static boolean autoConnect = false;
    private static String playerName = "Unknown";

    public static void init(FMLPreInitializationEvent event) {
        File configFile = new File(event.getModConfigurationDirectory(), "fragtone.cfg");
        config = new Configuration(configFile);
        loadConfig();
    }

    public static void loadConfig() {
        config.load();

        Property websocketUrlProperty = config.get(
                Configuration.CATEGORY_GENERAL,
                "websocketUrl",
                "ws://localhost:8080",
                "WebSocket server URL to connect to"
        );
        websocketUrl = websocketUrlProperty.getString();

        Property autoConnectProperty = config.get(
                Configuration.CATEGORY_GENERAL,
                "autoConnect",
                false,
                "Automatically connect to WebSocket server on game start"
        );
        autoConnect = autoConnectProperty.getBoolean();

        Property playerNameProperty = config.get(
                Configuration.CATEGORY_GENERAL,
                "playerName",
                "Unknown",
                "Player name to use in WebSocket messages (defaults to in-game name if left as 'Unknown')"
        );
        playerName = playerNameProperty.getString();

        if (config.hasChanged()) {
            config.save();
        }
    }

    public static void saveConfig() {
        config.get(Configuration.CATEGORY_GENERAL, "websocketUrl", "ws://localhost:8080").set(websocketUrl);
        config.get(Configuration.CATEGORY_GENERAL, "autoConnect", false).set(autoConnect);
        config.get(Configuration.CATEGORY_GENERAL, "playerName", "Unknown").set(playerName);

        if (config.hasChanged()) {
            config.save();
        }
    }

    public static String getWebsocketUrl() {
        return websocketUrl;
    }

    public static void setWebsocketUrl(String url) {
        websocketUrl = url;
        saveConfig();
    }

    public static boolean isAutoConnect() {
        return autoConnect;
    }

    public static void setAutoConnect(boolean value) {
        autoConnect = value;
        saveConfig();
    }

    public static String getPlayerName() {
        return playerName;
    }

    public static void setPlayerName(String name) {
        playerName = name;
        saveConfig();
    }
}
