package com.tramchester.graph;

import org.neo4j.graphalgo.WeightedPath;
import org.neo4j.graphalgo.impl.util.WeightedPathImpl;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.traversal.Traverser;
import org.neo4j.kernel.impl.traversal.MonoDirectionalTraversalDescription;

import java.util.Collections;

import static com.tramchester.graph.TransportRelationshipTypes.*;

public class TramNetworkTraverser {


    private final PathExpander<Double> pathExpander;

    public TramNetworkTraverser(PathExpander<Double> pathExpander) {
        this.pathExpander = pathExpander;
    }

    public Iterable<WeightedPath> findPaths(Node startNode, Node endNode) {
        Traverser traverser = new MonoDirectionalTraversalDescription().
                expand(pathExpander).depthFirst().
                traverse(startNode);

        ResourceIterator<Path> iterator = traverser.iterator();
        while (iterator.hasNext()) {
            Path path = iterator.next();
            if (path.endNode().getId()==endNode.getId()) {
                return Collections.singletonList(calculateWeight(path));
            }
        }

        return Collections.emptyList();
    }

    private WeightedPath calculateWeight(Path path) {
        Integer result = 0;
        for (Relationship relat: path.relationships()) {
            result = result + (int)relat.getProperty(GraphStaticKeys.COST);
        }
        return new WeightedPathImpl(result.doubleValue(), path);

    }
}
