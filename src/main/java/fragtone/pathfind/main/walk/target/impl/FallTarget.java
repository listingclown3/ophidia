package fragtone.pathfind.main.walk.target.impl;

import net.minecraft.util.BlockPos;
import net.minecraft.util.Vec3;
import fragtone.util.Util;
import fragtone.pathfind.main.path.PathElm;
import fragtone.pathfind.main.path.impl.FallNode;
import fragtone.pathfind.main.walk.target.WalkTarget;

public class FallTarget extends WalkTarget {

    FallNode node;
    public FallTarget(FallNode node) {
        this.node = node;
    }
    @Override
    public boolean tick(Vec3 predictedMotionOnStop, Vec3 playerPos) {
        setCurrentTarget(node.getBlockPos());

        playerPos = new Vec3(playerPos.xCoord, 0, playerPos.zCoord);
        Vec3 dest = new Vec3(node.getX(), 0, node.getZ()).addVector(0.5d, 0d, 0.5d);
        double predicatedPositionDistance = playerPos.distanceTo(playerPos.add(predictedMotionOnStop));
        double destPositionDistance = playerPos.distanceTo(dest);

        double angle = calculateAnglePredictionDest(predictedMotionOnStop, dest.subtract(playerPos));
        System.out.println("TEST" + " " + Util.toBlockPos(playerPos) + " " + (Util.toBlockPos(dest)));
        return (predicatedPositionDistance > destPositionDistance   && angle < PREDICTED_MOTION_ANGLE) || Util.toBlockPos(playerPos).equals(Util.toBlockPos(dest));
    }

    public BlockPos getNodeBlockPos() {
        return node.getBlockPos();
    }

    public PathElm getElm() {
        return node;
    }
}
