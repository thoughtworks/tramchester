package com.tramchester.graph.search;

import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.graph.FindStationsByNumberConnections;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.GraphPropertyKey;
import com.tramchester.graph.graphbuild.GraphBuilder;
import com.tramchester.graph.graphbuild.GraphProps;
import com.tramchester.graph.graphbuild.StationsAndLinksGraphBuilder;
import com.tramchester.repository.StationRepository;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static java.lang.String.format;

public class FindStationLinks {
    private static final Logger logger = LoggerFactory.getLogger(FindStationsByNumberConnections.class);

    private final GraphDatabase graphDatabase;
    private final StationRepository stationRepository;

    @Inject
    public FindStationLinks(GraphDatabase graphDatabase, StationsAndLinksGraphBuilder.Ready readyToken, StationRepository stationRepository) {
        this.graphDatabase = graphDatabase;
        this.stationRepository = stationRepository;
    }

    public Set<StationLink> findFor(TransportMode mode) {
        logger.info(format("Find links for %s", mode));
        long start = System.currentTimeMillis();
        Map<String, Object> params = new HashMap<>();
        String stationLabel = GraphBuilder.Labels.forMode(mode).name();
        String modesProps = GraphPropertyKey.TRANSPORT_MODES.getText();

        params.put("mode", mode.getNumber());

        String query = format("MATCH (a:%s)-[r:LINKED]->(b) " +
                        "WHERE $mode in r.%s " +
                        "RETURN r",
                stationLabel, modesProps);

        logger.info("Query: '" + query + '"');

        Set<StationLink> links = new HashSet<>();
        try (Transaction txn  = graphDatabase.beginTx()) {
            Result result = txn.execute(query, params);
            while (result.hasNext()) {
                Map<String, Object> row = result.next();
                Relationship relationship = (Relationship) row.get("r");
                Node startNode = relationship.getStartNode();
                Node endNode = relationship.getEndNode();

                links.add(createLink(startNode, endNode));
            }
            result.close();
        }
        long duration = System.currentTimeMillis()-start;
        logger.info("Took " + duration);
        logger.info("Found " + links.size() + " links");
        return links;
    }

    private StationLink createLink(Node startNode, Node endNode) {
        IdFor<Station> startId = GraphProps.getStationId(startNode);
        IdFor<Station> endId = GraphProps.getStationId(endNode);

        Station start = stationRepository.getStationById(startId);
        Station end = stationRepository.getStationById(endId);

        return new StationLink(start, end);
    }

    public static class StationLink {
        private final Station begin;
        private final Station end;

        public StationLink(Station begin, Station end) {
            this.begin = begin;
            this.end = end;
        }

        public Station getBegin() {
            return begin;
        }

        public Station getEnd() {
            return end;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            StationLink that = (StationLink) o;

            if (!begin.equals(that.begin)) return false;
            return end.equals(that.end);
        }

        @Override
        public int hashCode() {
            int result = begin.hashCode();
            result = 31 * result + end.hashCode();
            return result;
        }

        @Override
        public String toString() {
            return "StationLink{" +
                    "begin=" + begin +
                    ", end=" + end +
                    '}';
        }
    }

}
