package fragtone.websockets;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import fragtone.FragtoneConfig;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentText;
import net.minecraft.command.ICommand;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.BlockPos;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;

import static fragtone.websockets.ActionHandler.handleGotoAction;

public class WebSocketManager {
    private MinecraftWebSocketClient client;
    private boolean isConnected = false;
    private static WebSocketManager instance;
    private Gson gson = new Gson();

    public static WebSocketManager getInstance() {
        if (instance == null) {
            instance = new WebSocketManager();
        }
        return instance;
    }

    public void connect() {
        if (isConnected) {
            printToChat("Already connected to WebSocket server");
            return;
        }

        try {
            String serverUrl = FragtoneConfig.getWebsocketUrl();
            client = new MinecraftWebSocketClient(new URI(serverUrl));
            printToChat("Connecting to " + serverUrl + "...");
            client.connect();
        } catch (URISyntaxException e) {
            printToChat("Error connecting to WebSocket server: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void disconnect() {
        if (client != null && isConnected) {
            client.close();
            isConnected = false;
            printToChat("Disconnected from WebSocket server");
        } else {
            printToChat("Not connected to WebSocket server");
        }
    }

    public void sendMessage(String message) {
        if (client != null && isConnected) {
            try {
                JsonObject messageObj = new JsonObject();
                messageObj.addProperty("type", "chat");
                messageObj.addProperty("sender", getPlayerName());
                messageObj.addProperty("content", message);

                client.send(gson.toJson(messageObj));
            } catch (Exception e) {
                printToChat("Error sending message: " + e.getMessage());
            }
        } else {
            printToChat("Not connected to WebSocket server. Use /ws connect first");
        }
    }

    public void sendAction(String action, String data) {
        if (client != null && isConnected) {
            try {
                JsonObject messageObj = new JsonObject();
                messageObj.addProperty("type", "action");
                messageObj.addProperty("sender", getPlayerName());
                messageObj.addProperty("action", action);
                messageObj.addProperty("data", data);

                client.send(gson.toJson(messageObj));
            } catch (Exception e) {
                printToChat("Error sending action: " + e.getMessage());
            }
        } else {
            printToChat("Not connected to WebSocket server. Use /ws connect first");
        }
    }

    public boolean isConnected() {
        return isConnected;
    }

    private String getPlayerName() {
        String configName = FragtoneConfig.getPlayerName();
        if (configName != null && !configName.equals("Unknown")) {
            return configName;
        }

        if (Minecraft.getMinecraft().thePlayer != null) {
            return Minecraft.getMinecraft().thePlayer.getName();
        }

        return "Unknown";
    }

    public static void printToChat(String message) {
        if (Minecraft.getMinecraft().thePlayer != null) {
            Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText("§a[WebSocket] §f" + message));
        } else {
            System.out.println("[WebSocket] " + message);
        }
    }

    private class MinecraftWebSocketClient extends WebSocketClient {
        public MinecraftWebSocketClient(URI serverUri) {
            super(serverUri);
        }

        @Override
        public void onOpen(ServerHandshake handshakedata) {
            isConnected = true;
            printToChat("Connected to WebSocket server");

            // Send player login notification
            JsonObject messageObj = new JsonObject();
            messageObj.addProperty("type", "system");
            messageObj.addProperty("sender", getPlayerName());
            messageObj.addProperty("content", "has connected");
            send(gson.toJson(messageObj));
        }

        @Override
        public void onMessage(String message) {
            try {
                // First parse the raw message into JSON
                JsonObject messageObj = gson.fromJson(message, JsonObject.class);

                // Validate basic message structure
                if (messageObj == null) {
                    printToChat("§cReceived empty message");
                    return;
                }

                // Check required fields for all message types
                if (!messageObj.has("type")) {
                    printToChat("§cAll messages must include a 'type' field");
                    return;
                }

                String type = messageObj.get("type").getAsString();
                String sender = messageObj.has("sender") ? messageObj.get("sender").getAsString() : "unknown";

                // Handle different message types
                if ("chat".equalsIgnoreCase(type)) {
                    handleChatMessage(messageObj, sender);
                } else if ("action".equalsIgnoreCase(type)) {
                    handleActionMessage(messageObj, sender);
                } else if ("system".equalsIgnoreCase(type)) {
                    handleSystemMessage(messageObj, sender);
                } else if ("error".equalsIgnoreCase(type)) {
                    handleErrorMessage(messageObj);
                } else {
                    printToChat("§eReceived unknown message type: " + type);
                    if (messageObj.has("content")) {
                        printToChat("§fContent: " + messageObj.get("content").getAsString());
                    }
                }
            } catch (JsonSyntaxException e) {
                printToChat("§cInvalid JSON format: " + message);
            } catch (Exception e) {
                printToChat("§cUnexpected error: " + e.getMessage());
                e.printStackTrace();
            }
        }

        private void handleChatMessage(JsonObject messageObj, String sender) {
            if (!messageObj.has("content")) {
                printToChat("§cChat message missing content");
                return;
            }
            String content = messageObj.get("content").getAsString();
            printToChat("§b" + sender + "§f: " + content);
        }

        private void handleActionMessage(JsonObject messageObj, String sender) {
            if (!messageObj.has("action") || !messageObj.has("data")) {
                printToChat("§cAction message requires both 'action' and 'data' fields");
                return;
            }

            String action = messageObj.get("action").getAsString();
            JsonElement data = messageObj.get("data");

            if (!data.isJsonObject()) {
                printToChat("§cAction data must be a JSON object");
                return;
            }

            JsonObject actionData = data.getAsJsonObject();
            JsonElement dataElement = messageObj.has("data") ? messageObj.get("data") : null;

            // Handle specific actions
            if ("GOTO".equalsIgnoreCase(action)) {
                if (dataElement == null || !dataElement.isJsonObject()) {
                    printToChat("§cGOTO action requires coordinate data");
                    return;
                }
                ActionHandler.handleGotoAction(dataElement.getAsJsonObject());
            } else {
                printToChat("§d" + sender + " §fperformed: §e" + action);
                if (!actionData.entrySet().isEmpty()) {
                    printToChat("§7Data: " + actionData.toString());
                }
            }
        }

        private void handleSystemMessage(JsonObject messageObj, String sender) {
            String content = messageObj.has("content") ?
                    messageObj.get("content").getAsString() : "";

            if ("server".equalsIgnoreCase(sender)) {
                printToChat("§6[Server] §f" + content);
            } else {
                printToChat("§6[" + sender + "] §f" + content);
            }
        }

        private void handleErrorMessage(JsonObject messageObj) {
            String errorMsg = messageObj.has("content") ?
                    messageObj.get("content").getAsString() : "Unknown error occurred";

            printToChat("§c[Error] §f" + errorMsg);

            if (messageObj.has("details")) {
                printToChat("§7Details: " + messageObj.get("details").getAsString());
            }
        }

        @Override
        public void onClose(int code, String reason, boolean remote) {
            isConnected = false;
            printToChat("Disconnected from server: " + reason);
        }

        @Override
        public void onError(Exception ex) {
            isConnected = false;
            printToChat("Error: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        // Auto-reconnect logic could be added here if desired
    }

    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        // Auto-connect if enabled in config
        if (FragtoneConfig.isAutoConnect()) {
            connect();
        }
    }

    public static class WebSocketCommand implements ICommand {
        private final List<String> aliases;

        public WebSocketCommand() {
            aliases = new ArrayList();
            aliases.add("websocket");
        }

        @Override
        public String getCommandName() {
            return "ws";
        }

        @Override
        public String getCommandUsage(ICommandSender sender) {
            return "/ws <connect|disconnect|send|action|config> [args]";
        }

        @Override
        public List<String> getCommandAliases() {
            return aliases;
        }

        @Override
        public void processCommand(ICommandSender sender, String[] args) throws CommandException {
            if (args.length == 0) {
                sender.addChatMessage(new ChatComponentText("§a[WebSocket] §fUsage: " + getCommandUsage(sender)));
                return;
            }

            WebSocketManager manager = WebSocketManager.getInstance();

            String subCommand = args[0].toLowerCase();

            if ("connect".equals(subCommand)) {
                manager.connect();
            } else if ("disconnect".equals(subCommand)) {
                manager.disconnect();
            } else if ("send".equals(subCommand)) {
                if (args.length < 2) {
                    sender.addChatMessage(new ChatComponentText("§a[WebSocket] §fUsage: /ws send <message>"));
                    return;
                }

                StringBuilder messageBuilder = new StringBuilder();
                for (int i = 1; i < args.length; i++) {
                    messageBuilder.append(args[i]).append(" ");
                }
                String message = messageBuilder.toString().trim();
                manager.sendMessage(message);

            } else if ("action".equals(subCommand)) {
                if (args.length < 3) {
                    sender.addChatMessage(new ChatComponentText("§a[WebSocket] §fUsage: /ws action <action_name> <action_data>"));
                    return;
                }
                String action = args[1];

                StringBuilder dataBuilder = new StringBuilder();
                for (int i = 2; i < args.length; i++) {
                    dataBuilder.append(args[i]).append(" ");
                }
                String data = dataBuilder.toString().trim();

                manager.sendAction(action, data);

            } else if ("config".equals(subCommand)) {
                if (args.length < 2) {
                    sender.addChatMessage(new ChatComponentText("§a[WebSocket] §fUsage: /ws config <url|autoconnect|name> [value]"));
                    sender.addChatMessage(new ChatComponentText("§a[WebSocket] §fCurrent URL: " + FragtoneConfig.getWebsocketUrl()));
                    sender.addChatMessage(new ChatComponentText("§a[WebSocket] §fAuto Connect: " + FragtoneConfig.isAutoConnect()));
                    sender.addChatMessage(new ChatComponentText("§a[WebSocket] §fPlayer Name: " + FragtoneConfig.getPlayerName()));
                    return;
                }

                String configOption = args[1].toLowerCase();

                if ("url".equals(configOption)) {
                    if (args.length < 3) {
                        sender.addChatMessage(new ChatComponentText("§a[WebSocket] §fCurrent URL: " + FragtoneConfig.getWebsocketUrl()));
                        return;
                    }
                    FragtoneConfig.setWebsocketUrl(args[2]);
                    sender.addChatMessage(new ChatComponentText("§a[WebSocket] §fURL set to: " + args[2]));

                } else if ("autoconnect".equals(configOption)) {
                    if (args.length < 3) {
                        sender.addChatMessage(new ChatComponentText("§a[WebSocket] §fAuto Connect: " + FragtoneConfig.isAutoConnect()));
                        return;
                    }
                    boolean autoConnect = Boolean.parseBoolean(args[2]);
                    FragtoneConfig.setAutoConnect(autoConnect);
                    sender.addChatMessage(new ChatComponentText("§a[WebSocket] §fAuto Connect set to: " + autoConnect));

                } else if ("name".equals(configOption)) {
                    if (args.length < 3) {
                        sender.addChatMessage(new ChatComponentText("§a[WebSocket] §fPlayer Name: " + FragtoneConfig.getPlayerName()));
                        return;
                    }
                    FragtoneConfig.setPlayerName(args[2]);
                    sender.addChatMessage(new ChatComponentText("§a[WebSocket] §fPlayer Name set to: " + args[2]));

                } else {
                    sender.addChatMessage(new ChatComponentText("§a[WebSocket] §fUnknown config option. Valid options: url, autoconnect, name"));
                }

            } else {
                sender.addChatMessage(new ChatComponentText("§a[WebSocket] §fUnknown command. Usage: " + getCommandUsage(sender)));
            }
        }

            @Override
        public boolean canCommandSenderUseCommand(ICommandSender sender) {
            return true;
        }

        @Override
        public List<String> addTabCompletionOptions(ICommandSender sender, String[] args, BlockPos pos) {
            List<String> options = new ArrayList();
            if (args.length == 1) {
                options.add("connect");
                options.add("disconnect");
                options.add("send");
                options.add("action");
                options.add("config");
            } else if (args.length == 2 && args[0].equalsIgnoreCase("config")) {
                options.add("url");
                options.add("autoconnect");
                options.add("name");
            } else if (args.length == 3 && args[0].equalsIgnoreCase("config") && args[1].equalsIgnoreCase("autoconnect")) {
                options.add("true");
                options.add("false");
            }
            return options;
        }

        @Override
        public boolean isUsernameIndex(String[] args, int index) {
            return false;
        }

        @Override
        public int compareTo(ICommand o) {
            return getCommandName().compareTo(o.getCommandName());
        }
    }
}