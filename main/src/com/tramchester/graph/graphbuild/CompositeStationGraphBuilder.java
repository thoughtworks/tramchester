package com.tramchester.graph.graphbuild;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.places.StationGroup;
import com.tramchester.domain.places.NaptanArea;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.geo.CoordinateTransforms;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.filters.GraphFilter;
import com.tramchester.metrics.TimedTransaction;
import com.tramchester.repository.StationGroupsRepository;
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
    private final StationGroupsRepository stationGroupsRepository;
    private final TramchesterConfig config;
    private final GraphFilter graphFilter;
    private final GraphBuilderCache builderCache;

    // NOTE: cannot use graphquery here as creates a circular dependency on this class
    //private final GraphQuery graphQuery;

    @Inject
    public CompositeStationGraphBuilder(GraphDatabase graphDatabase, StationGroupsRepository stationGroupsRepository,
                                        TramchesterConfig config, GraphFilter graphFilter,
                                        StationsAndLinksGraphBuilder.Ready stationsAndLinksAreBuilt,
                                        GraphBuilderCache builderCache) {
        super(graphDatabase);
        this.graphDatabase = graphDatabase;
        this.stationGroupsRepository = stationGroupsRepository;
        this.config = config;
        this.graphFilter = graphFilter;
        this.builderCache = builderCache;
    }

    @PostConstruct
    private void start() {
        logger.info("starting");
        addCompositesStationsToDB();
        logger.info("started");
    }

    private void addCompositesStationsToDB() {
        if (!stationGroupsRepository.isEnabled()) {
            logger.warn("Disabled, StationGroupsRepository is not enabled");
            return;
        }
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
        Set<StationGroup> allComposite = stationGroupsRepository.getStationGroupsFor(mode);

        if (allComposite.isEmpty()) {
            logger.info("No composite stations to add for " + mode);
            return;
        }

        final String logMessage = "adding " + allComposite.size() + " composite stations for " + mode;

        try(TimedTransaction timedTransaction = new TimedTransaction(graphDatabase, logger, logMessage)) {
            Transaction txn = timedTransaction.transaction();
            allComposite.stream().filter(graphFilter::shouldInclude).
                filter(this::shouldInclude).
                forEach(compositeStation -> {
                    Node stationNode = createGroupedStationNodes(txn, compositeStation);
                    linkStations(txn, stationNode, compositeStation);
            });
            timedTransaction.commit();
        }
    }

    private boolean shouldInclude(StationGroup station) {
        return graphFilter.shouldIncludeRoutes(station.getPickupRoutes()) ||
                graphFilter.shouldIncludeRoutes(station.getDropoffRoutes());
    }

    private Node createGroupedStationNodes(Transaction txn, StationGroup stationGroup) {
        Node groupNode = createGraphNode(txn, GraphLabel.GROUPED);
        IdFor<NaptanArea> areaId = stationGroup.getAreaId();
        GraphProps.setProperty(groupNode, areaId);
        GraphProps.setProperty(groupNode, stationGroup);
        return groupNode;
    }

    private void linkStations(Transaction txn, Node parentNode, StationGroup stationGroup) {
        Set<Station> contained = stationGroup.getContained();
        double mph = config.getWalkingMPH();

        contained.stream().
                filter(graphFilter::shouldInclude).
                forEach(station -> {
                    int walkingCost = CoordinateTransforms.calcCostInMinutes(stationGroup.getLatLong(), station, mph);
                    Node childNode = builderCache.getStation(txn, station.getId());
                    if (childNode==null) {
                        throw new RuntimeException("cannot find node for " + station);
                    }

                    addGroupRelationshipTowardsChild(parentNode, childNode, walkingCost);
                    addGroupRelationshipTowardsParent(childNode, parentNode, walkingCost);
        });
    }

    private boolean hasDBFlag(Transaction txn) {
        ResourceIterator<Node> query = graphDatabase.findNodes(txn, GraphLabel.COMPOSITES_ADDED);
        List<Node> nodes = query.stream().collect(Collectors.toList());
        return !nodes.isEmpty();
    }

    private void addDBFlag(Transaction txn) {
        txn.createNode(GraphLabel.COMPOSITES_ADDED);
    }

    public static class Ready {
        private Ready() {
        }
    }

}
