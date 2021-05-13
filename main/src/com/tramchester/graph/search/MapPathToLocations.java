package com.tramchester.graph.search;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.StringIdFor;
import com.tramchester.domain.places.Location;
import com.tramchester.domain.places.MyLocation;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.graph.graphbuild.GraphProps;
import com.tramchester.repository.CompositeStationRepository;
import com.tramchester.repository.StationRepository;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.tramchester.graph.graphbuild.GraphBuilder.Labels.*;

@LazySingleton
public class MapPathToLocations {
    private final CompositeStationRepository stationRepository;
    private final ObjectMapper mapper;

    @Inject
    public MapPathToLocations(CompositeStationRepository stationRepository, ObjectMapper mapper) {
        this.stationRepository = stationRepository;
        this.mapper = mapper;
    }

    public List<Location<?>> mapToLocations(Path path) {
        Location<?> previous = null;
        List<Location<?>> results = new ArrayList<>();
        for(Node node : path.nodes()) {
            Optional<Location<?>> maybeLocation = mapNode(node);
            maybeLocation.ifPresent(location -> {});
            if (maybeLocation.isPresent()) {
                Location<?> location = maybeLocation.get();
                if (results.isEmpty()) {
                    results.add(location);
                } else  {
                    if (!location.equals(previous)) {
                        results.add(location);
                    }
                }
                previous = location;
            }
        }
        return results;
    }

    private Optional<Location<?>> mapNode(Node node) {
        if (isStationNode(node)) {
            IdFor<Station> stationId = GraphProps.getStationIdFrom(node);
            return Optional.of(stationRepository.getStationById(stationId));
        }
        if (node.hasLabel(ROUTE_STATION)) {
            IdFor<Station> stationId = GraphProps.getStationIdFrom(node);
            return  Optional.of(stationRepository.getStationById(stationId));
        }
        if (node.hasLabel(QUERY_NODE)) {
            LatLong latLong = GraphProps.getLatLong(node);
            return  Optional.of(MyLocation.create(mapper, latLong));
        }
        return Optional.empty();
    }

    private boolean isStationNode(Node node) {
        return node.hasLabel(BUS_STATION) || node.hasLabel(TRAM_STATION) || node.hasLabel(TRAIN_STATION)
                || node.hasLabel(FERRY_STATION) || node.hasLabel(SUBWAY_STATION);
    }

}
