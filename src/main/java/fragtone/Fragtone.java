package fragtone;

import fragtone.websockets.ActionHandler;
import net.minecraft.command.ICommand;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import fragtone.pathfind.TravelCommand;
import fragtone.pathfind.main.LookManager;
import fragtone.pathfind.main.PathRenderer;
import fragtone.pathfind.main.walk.Walker;
import fragtone.websockets.WebSocketManager;
import fragtone.FragtoneConfig;
import java.util.Arrays;
import java.util.function.Consumer;

@Mod(modid = Fragtone.MODID, version = Fragtone.VERSION)
public class Fragtone
{
    public static final String MODID = "Fragtone";
    public static final String VERSION = "1.0";

    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        // Initialize configuration
        FragtoneConfig.init(event);
        System.out.println("[Fragtone] Configuration initialized");
    }

    @EventHandler
    public void init(FMLInitializationEvent event) {
        // Register existing commands and listeners

        ActionHandler.initialize();

        registerCommands(new TravelCommand());

        // Register WebSocket command
        registerCommands(new WebSocketManager.WebSocketCommand());

        // Register all event listeners
        registerListeners(
                this,
                new PathRenderer(),
                new Walker(),
                new LookManager(),
                WebSocketManager.getInstance() // Register WebSocket manager for events
        );

        // Log startup info
        System.out.println("[Fragtone] WebSocket functionality initialized.");
        System.out.println("[Fragtone] WebSocket server URL: " + FragtoneConfig.getWebsocketUrl());
        System.out.println("[Fragtone] Auto-connect enabled: " + FragtoneConfig.isAutoConnect());
    }

    private void registerCommands(ICommand... commands) {
        for (ICommand command : commands) {
            ClientCommandHandler.instance.registerCommand(command);
            System.out.println("[Fragtone] Registered command: " + command.getCommandName());
        }
    }

    private void registerListeners(final Object... listeners) {
        Arrays.stream(listeners).forEach(new Consumer<Object>() {
            @Override
            public void accept(Object listener) {
                MinecraftForge.EVENT_BUS.register(listener);
                System.out.println("[Fragtone] Registered event listener: " + listener.getClass().getSimpleName());
            }
        });
    }
}