package fragtone.websockets;

import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ChatComponentText;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import fragtone.pathfind.main.walk.Walker;

public class ActionHandler {
    private static BlockPos targetPos;
    private static volatile boolean isActive = false;
    private static final Object lock = new Object();

    static {
        MinecraftForge.EVENT_BUS.register(new ActionHandler());
    }

    public static void initialize() {
        // Static block already registers, but you can add additional setup here
    }

    public static void handleGotoAction(JsonObject data) {
        try {
            int x = data.get("x").getAsInt();
            int y = data.get("y").getAsInt();
            int z = data.get("z").getAsInt();
            targetPos = new BlockPos(x, y, z);
            isActive = true;

            ClientCommandHandler.instance.executeCommand(FMLClientHandler.instance().getClientPlayerEntity(), "/travel " + x + " " + y + " " + z);

        } catch (JsonSyntaxException e) {
            sendMessage("§5[Fragtone]§7 Invalid goto action: " + e.getMessage());
        }
    }

    private static void sendMessage(String message) {
        EntityPlayerSP player = Minecraft.getMinecraft().thePlayer;
        if (player != null) {
            player.addChatMessage(new ChatComponentText(message));
        }
    }

    private static void sendError(String message) {
        sendMessage("§c" + message);
    }
}