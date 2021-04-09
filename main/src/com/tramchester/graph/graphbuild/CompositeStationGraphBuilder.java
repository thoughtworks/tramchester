package com.tramchester.graph.graphbuild;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.places.CompositeStation;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.geo.CoordinateTransforms;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.GraphQuery;
import com.tramchester.graph.NodeTypeRepository;
import com.tramchester.repository.CompositeStationRepository;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.graphdb.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/***
 * Add nodes and relationships for composite stations to the exitsing graph
 */
@LazySingleton
public class CompositeStationGraphBuilder extends CreateNodesAndRelationships {
    private static final Logger logger = LoggerFactory.getLogger(CompositeStationGraphBuilder.class);

    private final GraphDatabase graphDatabase;
    private final CompositeStationRepository stationRepository;
    private final TramchesterConfig config;
    private final GraphQuery graphQuery;

    @Inject
    public CompositeStationGraphBuilder(GraphDatabase graphDatabase, NodeTypeRepository nodeTypeRepository, CompositeStationRepository stationRepository,
                                        TramchesterConfig config, GraphQuery graphQuery,
                                        StagedTransportGraphBuilder.Ready mainGraphIsBuilt) {
        super(graphDatabase, nodeTypeRepository);
        this.graphDatabase = graphDatabase;
        this.stationRepository = stationRepository;
        this.config = config;
        this.graphQuery = graphQuery;
    }

    @PostConstruct
    private void start() {
        logger.info("starting");
        config.getTransportModes().forEach(this::addCompositeNodesAndLinks);
        reportStats();
        logger.info("started");
    }

    private void addCompositeNodesAndLinks(TransportMode mode) {
        Set<CompositeStation> allComposite = stationRepository.getCompositesFor(mode);

        try(Transaction txn = graphDatabase.beginTx()) {
            if (hasDBFlag(txn)) {
                logger.warn("Composites already added, DB flag is present");
            } else {
                logger.info("Adding composite stations to database");
                allComposite.forEach(compositeStation -> {
                    Node stationNode = createStationNode(txn, compositeStation);
                    Set<Station> contained = compositeStation.getContained();
                    linkStations(txn, stationNode, compositeStation, contained);
                });
                addDBFlag(txn);
                txn.commit();
            }
        }
    }

    private void linkStations(Transaction txn, Node startNode, CompositeStation start, Set<Station> stations) {
        double mph = config.getWalkingMPH();
        stations.forEach(station -> {
            int cost = CoordinateTransforms.calcCostInMinutes(start, station, mph);
            Node otherNode = graphQuery.getStationNode(txn, station);
            addNeighbourRelationship(startNode, otherNode, cost);
            addNeighbourRelationship(otherNode, startNode, cost);
        });
    }

    private boolean hasDBFlag(Transaction txn) {
        return graphQuery.hasAnyNodesWithLabel(txn, GraphBuilder.Labels.COMPOSITES_ADDED);
    }

    private void addDBFlag(Transaction txn) {
        txn.createNode(GraphBuilder.Labels.COMPOSITES_ADDED);
    }

}
