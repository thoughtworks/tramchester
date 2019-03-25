package com.tramchester.graph;

import com.tramchester.domain.Station;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.Relationship;

import static java.lang.String.format;

public class PathToGraphViz {

    private static String prefixToRemove = Station.METROLINK_PREFIX + "MA";

    public static String map(Path path) {
        StringBuilder builder = new StringBuilder();
        Iterable<Relationship> relationships = path.relationships();

        if (relationships!=null) {
            relationships.forEach(relationship -> {
                String start = relationship.getStartNode().getProperty(GraphStaticKeys.ID).toString().
                        replace(prefixToRemove, "");
                String end = relationship.getEndNode().getProperty(GraphStaticKeys.ID).toString().
                        replace(prefixToRemove, "");
                String relat = relationship.getType().name();
                builder.append(format("\"%s\"->\"%s\" [label=\"%s\"];\n", start, end, relat));

            });
        }

        return builder.toString();
    }
}
