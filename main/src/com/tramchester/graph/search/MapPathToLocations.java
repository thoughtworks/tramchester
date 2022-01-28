package com.tramchester.graph.search;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.places.*;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.graph.caches.NodeContentsRepository;
import com.tramchester.graph.graphbuild.GraphLabel;
import com.tramchester.graph.graphbuild.GraphProps;
import com.tramchester.repository.StationGroupsRepository;
import com.tramchester.repository.StationRepository;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

import static com.tramchester.graph.graphbuild.GraphLabel.*;
import static java.lang.String.format;

@LazySingleton
public class MapPathToLocations {
    private final StationRepository stationRepository;
    private final NodeContentsRepository nodeContentsRepository;
    private final StationGroupsRepository stationGroupsRepository;

    @Inject
    public MapPathToLocations(StationRepository stationRepository, NodeContentsRepository nodeContentsRepository,
                              StationGroupsRepository stationGroupsRepository) {
        this.stationRepository = stationRepository;
        this.nodeContentsRepository = nodeContentsRepository;
        this.stationGroupsRepository = stationGroupsRepository;
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
        EnumSet<GraphLabel> labels = nodeContentsRepository.getLabels(node);
        if (labels.contains(GROUPED)) {
            IdFor<NaptanArea> areaId = GraphProps.getAreaIdFromGrouped(node);
            final StationGroup stationGroup = stationGroupsRepository.getStationGroup(areaId);
            if (stationGroup==null) {
                throw new RuntimeException(format("Missing grouped station %s for %s labels %s props %s",
                        areaId, node.getId(), node.getLabels(), node.getAllProperties()));
            }
            return Optional.of(stationGroup);
        }
        if (labels.contains(STATION)) {
            IdFor<Station> stationId = GraphProps.getStationIdFrom(node);
            return Optional.of(stationRepository.getStationById(stationId));
        }
        if (labels.contains(ROUTE_STATION)) {
            IdFor<Station> stationId = GraphProps.getStationIdFrom(node);
            return Optional.of(stationRepository.getStationById(stationId));
        }
        if (labels.contains(QUERY_NODE)) {
            LatLong latLong = GraphProps.getLatLong(node);
            return  Optional.of(MyLocation.create(latLong));
        }
        return Optional.empty();
    }

}
