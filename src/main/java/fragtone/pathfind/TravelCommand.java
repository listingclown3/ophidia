package fragtone.pathfind;

import net.minecraft.util.ChatComponentText;
import fragtone.util.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.BlockPos;
import fragtone.pathfind.main.walk.Walker;
import net.minecraftforge.fml.client.config.GuiConfigEntries;

public class TravelCommand extends CommandBase {
    public static BlockPos endPos;
    public static boolean wasInterrupted = false;
    public String getCommandName() {
        return "travel";
    }

    public String getCommandUsage(ICommandSender sender) {
        return null;
    }

    public boolean canCommandSenderUseCommand(ICommandSender sender) {
        return true;
    }

    public void processCommand(ICommandSender sender, String[] args) {
        BlockPos currentPos = Util.getPlayerBlockPos();
        wasInterrupted = false;

        if(args.length > 2) {

            int x = Integer.parseInt(args[0]);
            int y = Integer.parseInt(args[1]);
            int z = Integer.parseInt(args[2]);
            BlockPos targetPos = new BlockPos(x, y, z);
            endPos = targetPos;


            Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText("§5[§dFragtone§d]§5§7 Going from [" + currentPos.getX() + ", " + currentPos.getY() + ", " + currentPos.getZ() + "] to [" + targetPos.getX() + ", " + targetPos.getY() + ", " + targetPos.getZ() + "]"));
            Walker.getInstance().walk(currentPos, targetPos, 100000); // should thread this

        } else if (args.length == 1) {
            if (args[0].equalsIgnoreCase("stop")) {
                Walker.getInstance().walk(currentPos, currentPos, 10);
                Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText("§5[§dFragtone§d]§5§7 Stopping at [" + currentPos.getX() + ", " + currentPos.getY() + ", " + currentPos.getZ() + "]"));
                wasInterrupted = true;
            } else if (args[0].equalsIgnoreCase("help")) {
                Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText("§5[§dFragtone§d]§5§7 Usage: /travel <x>||<stop>||<continue> <y> <z>"));
            } else if (args[0].equalsIgnoreCase("continue")) {
                if (Walker.getInstance().isActive()) {
                    Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText("§5[§dFragtone§d]§5§7 Already on a path to [" + endPos.getX() + ", " + endPos.getY() + ", " + endPos.getZ() + "]"));
                } else {
                    Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText("§5[§dFragtone§d]§5§7 Resuming path from [" + currentPos.getX() + ", " + currentPos.getY() + ", " + currentPos.getZ() + "] to [" + endPos.getX() + ", " + endPos.getY() + ", " + endPos.getZ() + "]"));
                    Walker.getInstance().walk(currentPos, endPos, 100000);
                    wasInterrupted = false;
                }
            }

        } else {
            Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText("§5[§dFragtone§d]§5§7 Usage: /travel <x>||<stop>||<continue> <y> <z>"));
        }
    }
}