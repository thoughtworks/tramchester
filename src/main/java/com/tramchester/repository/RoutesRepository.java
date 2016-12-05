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
import org.neo4j.gis.spatial.SpatialDatabaseService;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public class RoutesRepository extends StationIndexs {

    private final StationRepository stationRepository;

    public RoutesRepository(GraphDatabaseService graphDatabaseService, RelationshipFactory relationshipFactory,
                            SpatialDatabaseService spatialDatabaseService,
                            StationRepository stationRepository) {
        super(graphDatabaseService, relationshipFactory, spatialDatabaseService, false);
        this.stationRepository = stationRepository;
    }

    public List<RouteDTO> getAllRoutes() {
        List<RouteDTO> results = new LinkedList<>();
        Collection<Route> routes = stationRepository.getRoutes();
        routes.forEach(route-> populate(route,results));
        return results;
    }

    private void populate(Route route, List<RouteDTO> gather) {
        List<StationDTO> stations = new LinkedList<>();
        String routeName = route.getName();
        try (Transaction tx = graphDatabaseService.beginTx()) {

            Stream<Node> nodes = super.getAll(route);
            nodes.forEach(node -> {
                Iterable<Relationship> departs = node.getRelationships(TransportRelationshipTypes.DEPART,
                        TransportRelationshipTypes.INTERCHANGE_DEPART);
                Node stationNode = departs.iterator().next().getEndNode();
                String id = (String) stationNode.getProperty(GraphStaticKeys.ID);
                Optional<Station> station = stationRepository.getStation(id);
                station.ifPresent(value -> stations.add(new StationDTO(value, ProximityGroup.ALL)));
            });
            tx.success();
        }

        gather.add(new RouteDTO(routeName, stations));

    }
}
