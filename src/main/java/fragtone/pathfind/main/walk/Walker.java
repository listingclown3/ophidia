package fragtone.pathfind.main.walk;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.Tuple;
import net.minecraft.util.Vec3;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;
import fragtone.util.LookUtil;
import fragtone.util.Util;
import fragtone.pathfind.main.LookManager;
import fragtone.pathfind.main.PathRenderer;
import fragtone.pathfind.main.processor.ProcessorManager;
import fragtone.pathfind.main.astar.AStarNode;
import fragtone.pathfind.main.astar.AStarPathFinder;
import fragtone.pathfind.main.path.PathElm;
import fragtone.pathfind.main.path.impl.FallNode;
import fragtone.pathfind.main.path.impl.JumpNode;
import fragtone.pathfind.main.path.impl.TravelNode;
import fragtone.pathfind.main.path.impl.TravelVector;
import fragtone.pathfind.main.walk.target.WalkTarget;
import fragtone.pathfind.main.walk.target.impl.FallTarget;
import fragtone.pathfind.main.walk.target.impl.JumpTarget;
import fragtone.pathfind.main.walk.target.impl.TravelTarget;
import fragtone.pathfind.main.walk.target.impl.TravelVectorTarget;

import javax.vecmath.Tuple2d;
import java.util.List;

import static fragtone.pathfind.TravelCommand.endPos;
import static fragtone.pathfind.TravelCommand.wasInterrupted;

public class Walker {
    private static Walker instance;
    private boolean isActive;

    List<PathElm> path;
    WalkTarget currentTarget;

    public Walker() {
        instance = this;
    }

    public void walk(BlockPos start, BlockPos end, int nodeCount) {
        try {
            isActive = true;

            List<AStarNode> nodes = AStarPathFinder.compute(start, end, nodeCount);
            if (nodes == null) {
                isActive = false;
                currentTarget = null;
                path = null;
                Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText("§5[§dFragtone§d]§5§7 Path computation failed! (NULL_NODES)"));
                return;
            }

            path = ProcessorManager.process(nodes);
            if (path == null) {
                isActive = false;
                currentTarget = null;
                Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText("§5[§dFragtone§d]§5§7 Path processing failed! (NULL_PATH)"));
                return;
            }

            if(path.size() == 0) {
                isActive = false;
                currentTarget = null;
                path = null;
                Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText("§5[§dFragtone§d]§5§7 Path was not found! (PATH_WALK_0)"));
                return;
            }

            PathRenderer.getInstance().render(path);
            currentTarget = null;
        } catch (Exception e) {
            isActive = false;
            currentTarget = null;
            path = null;
            Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText("§5[§dFragtone§d]§5§7 Error during pathfinding: " + e.getMessage()));
            e.printStackTrace();
        }
    }

    // Key press in here
    @SubscribeEvent
    public void onClientTickPre(TickEvent.ClientTickEvent event) {
        if(event.phase == TickEvent.Phase.END || Minecraft.getMinecraft().thePlayer == null)
            return;

        if(!isActive)
            return;

        // Critical null checks to prevent crashes
        if (path == null || path.isEmpty()) {
            // Path is null or empty, stop walker
            isActive = false;
            currentTarget = null;
            path = null;
            // Reset key states
            KeyBinding.setKeyBindState(Keyboard.KEY_W, false);
            KeyBinding.setKeyBindState(Keyboard.KEY_A, false);
            KeyBinding.setKeyBindState(Keyboard.KEY_D, false);
            KeyBinding.setKeyBindState(Keyboard.KEY_S, false);
            KeyBinding.setKeyBindState(Keyboard.KEY_LCONTROL, false);
            return;
        }

        try {
            if(currentTarget == null) {
                PathElm firstElement = path.get(0);
                if (firstElement == null) {
                    // First path element is null, clean up and stop
                    isActive = false;
                    currentTarget = null;
                    path = null;
                    return;
                }
                currentTarget = getCurrentTarget(firstElement);
                if (currentTarget == null) {
                    // Could not create target, stop walker
                    isActive = false;
                    path = null;
                    return;
                }
            }

            WalkTarget playerOnTarget;
            if(!((playerOnTarget = onTarget()) == null))
                currentTarget = playerOnTarget;

            // Safety check before tick
            if (currentTarget == null) {
                isActive = false;
                path = null;
                return;
            }

            // while, so we don't skip ticks
            while (tick(currentTarget)) {
                // removes it
                if (path == null || path.isEmpty()) {
                    break;
                }

                path.remove(0);

                if(path.isEmpty()) {
                    isActive = false;
                    currentTarget = null;
                    path = null;
                    KeyBinding.setKeyBindState(Keyboard.KEY_W, false);
                    KeyBinding.setKeyBindState(Keyboard.KEY_A, false);
                    KeyBinding.setKeyBindState(Keyboard.KEY_D, false);
                    KeyBinding.setKeyBindState(Keyboard.KEY_S, false);
                    KeyBinding.setKeyBindState(Keyboard.KEY_LCONTROL, false);
                    LookManager.getInstance().cancel();

                    if (!(wasInterrupted)) {
                        if (endPos != null) {
                            Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText("§5[§dFragtone§d]§5§7 Arrived at destination! [" + endPos.getX() + ", " + endPos.getY() + ", " + endPos.getZ() + "]"));
                        } else {
                            Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText("§5[§dFragtone§d]§5§7 Arrived at destination!"));
                        }
                    }

                    wasInterrupted = false;
                    return;
                }

                PathElm nextElement = path.get(0);
                if (nextElement == null) {
                    // Next element is null, stop
                    isActive = false;
                    currentTarget = null;
                    path = null;
                    return;
                }

                currentTarget = getCurrentTarget(nextElement);
                if (currentTarget == null) {
                    // Could not create next target, stop
                    isActive = false;
                    path = null;
                    return;
                }
            }

            // Final safety check before continuing
            if (currentTarget == null || currentTarget.getCurrentTarget() == null) {
                isActive = false;
                path = null;
                return;
            }

            KeyBinding.setKeyBindState(Keyboard.KEY_LCONTROL, true);
            Tuple<Double, Double> angles = LookUtil.getAngles(currentTarget.getCurrentTarget());
            if (angles != null && angles.getFirst() != null) {
                LookManager.getInstance().setTarget(angles.getFirst().floatValue(), currentTarget instanceof JumpTarget ? -10 : 10);
                pressKeys(angles.getFirst().floatValue());
            }
        } catch (Exception e) {
            // Any exception during tick processing - stop walker safely
            isActive = false;
            currentTarget = null;
            path = null;
            KeyBinding.setKeyBindState(Keyboard.KEY_W, false);
            KeyBinding.setKeyBindState(Keyboard.KEY_A, false);
            KeyBinding.setKeyBindState(Keyboard.KEY_D, false);
            KeyBinding.setKeyBindState(Keyboard.KEY_S, false);
            KeyBinding.setKeyBindState(Keyboard.KEY_LCONTROL, false);

            Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText("§5[§dFragtone§d]§5§7 Walker error: " + e.getMessage()));
            e.printStackTrace();
        }
    }

    private void pressKeys(double targetYaw) {
        double difference = targetYaw - Minecraft.getMinecraft().thePlayer.rotationYaw;
        KeyBinding.setKeyBindState(Keyboard.KEY_W, false);
        KeyBinding.setKeyBindState(Keyboard.KEY_A, false);
        KeyBinding.setKeyBindState(Keyboard.KEY_S, false);
        KeyBinding.setKeyBindState(Keyboard.KEY_D, false);

        if(22.5 > difference && difference > -22.5) {   // Forwards
            KeyBinding.setKeyBindState(Keyboard.KEY_W, true);
        } else if(-22.5 > difference && difference > -67.5) {   // Forwards+Right
            KeyBinding.setKeyBindState(Keyboard.KEY_W, true);
            KeyBinding.setKeyBindState(Keyboard.KEY_A, true);
        } else if(-67.5 > difference && difference > -112.5) { // Right
            KeyBinding.setKeyBindState(Keyboard.KEY_A, true);
        } else if(-112.5 > difference && difference > -157.5) { // Backwards + Right
            KeyBinding.setKeyBindState(Keyboard.KEY_A, true);
            KeyBinding.setKeyBindState(Keyboard.KEY_S, true);
        } else if((-157.5 > difference && difference > -180) || (180 > difference && difference > 157.5)) { // Backwards
            KeyBinding.setKeyBindState(Keyboard.KEY_S, true);
        } else if(67.5 > difference && difference > 22.5) { // Forwards + Left
            KeyBinding.setKeyBindState(Keyboard.KEY_W, true);
            KeyBinding.setKeyBindState(Keyboard.KEY_D, true);
        } else if(112.5 > difference && difference > 67.5) { // Left
            KeyBinding.setKeyBindState(Keyboard.KEY_D, true);
        } else if(157.5 > difference && difference > 112.5) {  // Backwards+Left
            KeyBinding.setKeyBindState(Keyboard.KEY_S, true);
            KeyBinding.setKeyBindState(Keyboard.KEY_D, true);
        }
    }

    // This checks if the player is on any nodes further in the queue, which means the player, due to probably high speed, has skipped some. Then
    // this removes the nodes behind it and sets it as the current target.
    private WalkTarget onTarget() {
        if (path == null || path.isEmpty()) {
            return null;
        }

        try {
            for(int i = 0 ; i < path.size() ; i++) {
                PathElm elm = path.get(i);
                if (elm == null) continue;

                if(elm.playerOn(Minecraft.getMinecraft().thePlayer.getPositionVector())) {
                    // System.out.println("Returned true: " + elm);

                    if(currentTarget != null && elm == currentTarget.getElm())
                        return null;

                    // Get the next one if the player is on it
                    // if its travel vector, we don't get the next one, cos we need to go to the dest.
                    // if its jump, we don't get the next one, cos we need to jump.
                    if(path.size() > i + 1 && !(elm instanceof TravelVector) && !(elm instanceof JumpNode)) {
                        System.out.println("E");
                        path.subList(0, i + 1).clear();
                    } else {
                        path.subList(0, i).clear();
                    }

                    // cutting off might end jump target so stop jumping
                    KeyBinding.setKeyBindState(Keyboard.KEY_SPACE, false);

                    if (path.isEmpty()) {
                        return null;
                    }

                    PathElm nextElement = path.get(0);
                    if (nextElement == null) {
                        return null;
                    }

                    return getCurrentTarget(nextElement);
                }
            }
        } catch (Exception e) {
            // Error during onTarget check, return null to be safe
            e.printStackTrace();
        }

        return null;
    }

    // The return value of this is if the node has been satisfied, and the next one should be polled.
    private boolean tick(WalkTarget current) {
        if (current == null) {
            return false;
        }

        try {
            // We should improve the predicted motion calculation. Right now it's based on the estimate that the motion will last for 12 ticks, but this is different across speeds.
            Vec3 offset = new Vec3(Minecraft.getMinecraft().thePlayer.motionX, 0, Minecraft.getMinecraft().thePlayer.motionZ);
            Vec3 temp = offset;
            offset.add(temp);

            for(int i = 0 ; i < 12 ; i++) {
                // 0.54600006f is how much the motion stops after every tick after not moving.
                offset = offset.add((temp = Util.vecMultiply(temp, 0.54600006f)));
            }

            return current.tick(offset, Minecraft.getMinecraft().thePlayer.getPositionVector());
        } catch (Exception e) {
            // Error during tick, return false to be safe
            e.printStackTrace();
            return false;
        }
    }

    private WalkTarget getCurrentTarget(PathElm elm) {
        if (elm == null) {
            return null;
        }

        try {
            if(elm instanceof FallNode)
                return new FallTarget((FallNode) elm);
            if(elm instanceof TravelNode)
                return new TravelTarget((TravelNode) elm);
            if(elm instanceof TravelVector)
                return new TravelVectorTarget((TravelVector) elm);
            if(elm instanceof JumpNode) {
                if(path != null && path.size() > 1) {
                    PathElm nextElement = path.get(1);
                    if (nextElement != null) {
                        WalkTarget nextTarget = getCurrentTarget(nextElement);
                        return new JumpTarget((JumpNode) elm, nextTarget);
                    }
                }
                return new JumpTarget((JumpNode) elm, null);
            }
            // System.out.println("Wrong walk target");
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public boolean isActive() {
        return isActive;
    }

    public static Walker getInstance() {
        return instance;
    }

    /**
     * Force stop the walker and clean up state
     */
    public void forceStop() {
        isActive = false;
        currentTarget = null;
        path = null;
        // Reset all key states
        KeyBinding.setKeyBindState(Keyboard.KEY_W, false);
        KeyBinding.setKeyBindState(Keyboard.KEY_A, false);
        KeyBinding.setKeyBindState(Keyboard.KEY_D, false);
        KeyBinding.setKeyBindState(Keyboard.KEY_S, false);
        KeyBinding.setKeyBindState(Keyboard.KEY_LCONTROL, false);
        KeyBinding.setKeyBindState(Keyboard.KEY_SPACE, false);

        if (LookManager.getInstance() != null) {
            LookManager.getInstance().cancel();
        }
    }
}