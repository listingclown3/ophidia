package fragtone.pathfind.main.processor;

import fragtone.pathfind.main.astar.AStarNode;
import fragtone.pathfind.main.path.PathElm;
import fragtone.pathfind.main.path.impl.FallNode;
import fragtone.pathfind.main.path.impl.JumpNode;
import fragtone.pathfind.main.path.impl.TravelNode;
import fragtone.pathfind.main.processor.impl.FallProcessor;
import fragtone.pathfind.main.processor.impl.JumpProcessor;
import fragtone.pathfind.main.processor.impl.TravelProcessor;

import java.util.ArrayList;
import java.util.List;

public class ProcessorManager {



    public static List<PathElm> process(List<AStarNode> aStarNodes) {
        List<PathElm> pathElms = convertRepresentation(aStarNodes);

        List<Processor> processors = new ArrayList();
        processors.add(new TravelProcessor());
        processors.add(new FallProcessor());
        processors.add(new JumpProcessor());

        for(Processor processor : processors) {
            processor.process(pathElms);
        }

        return pathElms;
    }



    // convert from base form of AStarNode to normal nodes
    private static List<PathElm> convertRepresentation(List<AStarNode> aStarNodes) {
        List<PathElm> pathElms = new ArrayList();

        for(int i = 0 ; i < aStarNodes.size() ; i++) {
            AStarNode node = aStarNodes.get(i);

            if(node.isJumpNode()) {
                pathElms.add(new JumpNode(node.getX(), node.getY(), node.getZ()));
                continue;
            }

            if(node.isFallNode()) {
                pathElms.add(new FallNode(node.getX(), node.getY(), node.getZ()));

                // skip directly in front of fall, unless if its the last node
                if(i != aStarNodes.size() - 1)
                    i+=1;
                continue;
            }

            pathElms.add(new TravelNode(node.getX(), node.getY(), node.getZ()));
        }



        return pathElms;
    }
}
