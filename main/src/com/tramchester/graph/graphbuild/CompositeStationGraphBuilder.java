package com.tramchester.graph.graphbuild;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.places.CompositeStation;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.geo.CoordinateTransforms;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.NodeTypeRepository;
import com.tramchester.graph.filters.GraphFilter;
import com.tramchester.repository.CompositeStationRepository;
import com.tramchester.repository.RouteRepository;
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
    private final RouteRepository routeRepository;
    private final TramchesterConfig config;
    private final GraphFilter graphFilter;

    // NOTE: cannot use graphquery here as creates a circular dependency on this class
    //private final GraphQuery graphQuery;

    @Inject
    public CompositeStationGraphBuilder(GraphDatabase graphDatabase, NodeTypeRepository nodeTypeRepository,
                                        CompositeStationRepository stationRepository, RouteRepository routeRepository,
                                        TramchesterConfig config, GraphFilter graphFilter,
                                        StationsAndLinksGraphBuilder.Ready stationsAndLinksAreBuilt) {
        super(graphDatabase, nodeTypeRepository);
        this.graphDatabase = graphDatabase;
        this.stationRepository = stationRepository;
        this.routeRepository = routeRepository;
        this.config = config;
        this.graphFilter = graphFilter;
    }

    @PostConstruct
    private void start() {
        logger.info("starting");
        addCompositesStationsToDB();
        logger.info("started");
    }

    private void addCompositesStationsToDB() {
        try(Transaction txn = graphDatabase.beginTx()) {
            if (hasDBFlag(txn)) {
                logger.info("Already present in DB");
                return;
            }
        }
        config.getTransportModes().forEach(this::addCompositeNodesAndLinks);
        try(Transaction txn = graphDatabase.beginTx()) {
            addDBFlag(txn);
            txn.commit();
        }
        reportStats();
    }

    // force contsruction via guide to generate ready token, needed where no direct code dependency on this class
    public Ready getReady() {
        return new Ready();
    }

    private void addCompositeNodesAndLinks(TransportMode mode) {
        Set<CompositeStation> allComposite = stationRepository.getCompositesFor(mode);

        if (allComposite.isEmpty()) {
            logger.info("No composite stations to add for " + mode);
            return;
        }

        logger.info("Adding composite stations to database for mode " + mode);
        try(Transaction txn = graphDatabase.beginTx()) {
            allComposite.stream().filter(graphFilter::shouldInclude).
                filter(station -> graphFilter.shouldInclude(routeRepository, station.getRoutes())).
                forEach(compositeStation -> {
                    Node stationNode = createStationNode(txn, compositeStation);
                    linkStations(txn, mode, stationNode, compositeStation);
            });
            txn.commit();
        }
    }

    private void linkStations(Transaction txn, TransportMode mode, Node startNode, CompositeStation parent) {
        Set<Station> stations = parent.getContained();
        double mph = config.getWalkingMPH();

        stations.stream().
                filter(graphFilter::shouldInclude).
                filter(station -> graphFilter.shouldInclude(routeRepository, station.getRoutes())).
                forEach(station -> {
                    int cost = CoordinateTransforms.calcCostInMinutes(parent, station, mph);
                    Node otherNode = getStationNode(txn, mode, station);
                    if (otherNode==null) {
                        throw new RuntimeException("cannot find node for " + station);
                    }
                    addNeighbourRelationship(startNode, otherNode, cost);
                    addNeighbourRelationship(otherNode, startNode, cost);
        });
    }

    private Node getStationNode(Transaction txn, TransportMode mode, Station station) {
        GraphBuilder.Labels label = GraphBuilder.Labels.forMode(mode);
        return graphDatabase.findNode(txn, label, station.getProp().getText(), station.getId().getGraphId());
    }

    private boolean hasDBFlag(Transaction txn) {
        ResourceIterator<Node> query = graphDatabase.findNodes(txn, GraphBuilder.Labels.COMPOSITES_ADDED);
        List<Node> nodes = query.stream().collect(Collectors.toList());
        return !nodes.isEmpty();
    }

    private void addDBFlag(Transaction txn) {
        txn.createNode(GraphBuilder.Labels.COMPOSITES_ADDED);
    }

    public static class Ready {
        private Ready() {
        }
    }

}
