package com.tramchester.graph.search;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.domain.IdFor;
import com.tramchester.domain.places.Location;
import com.tramchester.domain.places.MyLocation;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.graph.GraphPropertyKey;
import com.tramchester.graph.graphbuild.GraphBuilder;
import com.tramchester.graph.graphbuild.GraphProps;
import com.tramchester.repository.StationRepository;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

import static com.tramchester.graph.GraphPropertyKey.LATITUDE;
import static com.tramchester.graph.GraphPropertyKey.LONGITUDE;
import static com.tramchester.graph.graphbuild.GraphBuilder.Labels.*;

@LazySingleton
public class MapPathToLocations {
    private final StationRepository stationRepository;
    private final ObjectMapper mapper;

    @Inject
    public MapPathToLocations(StationRepository stationRepository, ObjectMapper mapper) {
        this.stationRepository = stationRepository;
        this.mapper = mapper;
    }

    public List<Location<?>> mapToLocations(Path path) {
        List<Location<?>> results = new ArrayList<>();
        path.nodes().forEach(node -> mapNode(results, node));
        return results;
    }

    private void mapNode(List<Location<?>> results, Node node) {
        if (isStationNode(node)) {
            IdFor<Station> stationId = IdFor.getStationIdFrom(node);
            results.add(stationRepository.getStationById(stationId));
        }
        if (node.hasLabel(ROUTE_STATION)) {
            IdFor<Station> stationId = IdFor.getStationIdFrom(node);
            results.add(stationRepository.getStationById(stationId));
        } else if (node.hasLabel(QUERY_NODE)) {
            LatLong latLong = GraphProps.getLatLong(node);
            Location<MyLocation> location = MyLocation.create(mapper, latLong);
            results.add(location);
        }
    }

    private boolean isStationNode(Node node) {
        return node.hasLabel(BUS_STATION) || node.hasLabel(TRAM_STATION) || node.hasLabel(TRAIN_STATION)
                || node.hasLabel(FERRY_STATION) || node.hasLabel(SUBWAY_STATION);
    }

}
