package com.tramchester.graph.search;

import com.tramchester.domain.StationLink;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.graph.FindStationsByNumberLinks;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.GraphPropertyKey;
import com.tramchester.graph.graphbuild.GraphBuilder;
import com.tramchester.graph.graphbuild.GraphProps;
import com.tramchester.graph.graphbuild.StationsAndLinksGraphBuilder;
import com.tramchester.metrics.TimedTransaction;
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
    private static final Logger logger = LoggerFactory.getLogger(FindStationsByNumberLinks.class);

    private final GraphDatabase graphDatabase;
    private final StationRepository stationRepository;

    @Inject
    public FindStationLinks(GraphDatabase graphDatabase, StationsAndLinksGraphBuilder.Ready readyToken,
                            StationRepository stationRepository) {
        this.graphDatabase = graphDatabase;
        this.stationRepository = stationRepository;
    }

    public Set<StationLink> findLinkedFor(TransportMode mode) {
        logger.info(format("Find links for %s", mode));
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
        try (TimedTransaction timedTransaction = new TimedTransaction(graphDatabase, logger, "query for links " + mode)) {
            Transaction txn = timedTransaction.transaction();
            Result result = txn.execute(query, params);
            while (result.hasNext()) {
                Map<String, Object> row = result.next();
                Relationship relationship = (Relationship) row.get("r");

                Node startNode = relationship.getStartNode();
                Node endNode = relationship.getEndNode();

                links.add(createLink(startNode, endNode, relationship));
            }
            result.close();
        }

        logger.info("Found " + links.size() + " links");
        return links;
    }

    private StationLink createLink(Node startNode, Node endNode, Relationship relationship) {
        IdFor<Station> startId = GraphProps.getStationId(startNode);
        IdFor<Station> endId = GraphProps.getStationId(endNode);

        Station start = stationRepository.getStationById(startId);
        Station end = stationRepository.getStationById(endId);

        Set<TransportMode> modes = GraphProps.getTransportModes(relationship);

        return new StationLink(start, end, modes);
    }

}
