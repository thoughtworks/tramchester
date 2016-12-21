package com.tramchester.repository;

import com.tramchester.domain.Route;
import com.tramchester.domain.Station;
import com.tramchester.domain.presentation.DTO.RouteDTO;
import com.tramchester.domain.presentation.DTO.StationDTO;
import com.tramchester.domain.presentation.ProximityGroup;
import com.tramchester.graph.GraphStaticKeys;
import com.tramchester.graph.Relationships.RelationshipFactory;
import com.tramchester.graph.StationIndexs;
import com.tramchester.graph.TransportRelationshipTypes;
import com.tramchester.resources.RouteCodeToClassMapper;
import org.neo4j.gis.spatial.SpatialDatabaseService;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public class RoutesRepository extends StationIndexs {
    private static final Logger logger = LoggerFactory.getLogger(RoutesRepository.class);

    private final StationRepository stationRepository;
    private RouteCodeToClassMapper mapper;

    public RoutesRepository(GraphDatabaseService graphDatabaseService, RelationshipFactory relationshipFactory,
                            SpatialDatabaseService spatialDatabaseService,
                            StationRepository stationRepository,
                            RouteCodeToClassMapper mapper) {
        super(graphDatabaseService, relationshipFactory, spatialDatabaseService, false);
        this.stationRepository = stationRepository;
        this.mapper = mapper;
    }

    public List<RouteDTO> getAllRoutes() {
        List<RouteDTO> routeDTOs = new LinkedList<>();
        Collection<Route> routes = stationRepository.getRoutes();
        routes.forEach(route-> populateDTOFor(route,routeDTOs));
        logger.info(String.format("Found %s routes", routes.size()));
        return routeDTOs;
    }

    private void populateDTOFor(Route route, List<RouteDTO> gather) {
        List<StationDTO> stations = new LinkedList<>();
        String routeName = route.getName();
        logger.debug("Finding nodes for route "  + routeName);
        try (Transaction tx = graphDatabaseService.beginTx()) {
            Stream<Node> nodes = super.getNodesFor(route);
            nodes.forEach(node -> {
                Node stationNode = getTargetNode(node);
                String id = (String) stationNode.getProperty(GraphStaticKeys.ID);
                Optional<Station> station = stationRepository.getStation(id);
                station.ifPresent(value -> stations.add(new StationDTO(value, ProximityGroup.ALL)));
            });
            tx.success();
        }

        gather.add(new RouteDTO(routeName, stations, mapper.map(route.getId())));
    }

    private Node getTargetNode(Node node) {
        Iterable<Relationship> departs = node.getRelationships(TransportRelationshipTypes.DEPART,
                TransportRelationshipTypes.INTERCHANGE_DEPART);
        return departs.iterator().next().getEndNode();
    }
}
