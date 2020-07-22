package com.tramchester.graph.search;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tramchester.domain.places.Location;
import com.tramchester.domain.places.MyLocation;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.graph.GraphStaticKeys;
import com.tramchester.graph.graphbuild.GraphBuilder;
import com.tramchester.repository.StationRepository;
import org.neo4j.graphdb.Path;

import java.util.ArrayList;
import java.util.List;

import static com.tramchester.graph.GraphStaticKeys.STATION_ID;

public class MapPathToLocations {
    private final StationRepository stationRepository;
    private final ObjectMapper mapper;

    public MapPathToLocations(StationRepository stationRepository) {
        this.stationRepository = stationRepository;
        mapper = new ObjectMapper();
    }

    public List<Location> mapToLocations(Path path) {
        List<Location> results = new ArrayList<>();
        path.nodes().forEach(node -> {
            if (node.hasLabel(GraphBuilder.Labels.ROUTE_STATION)) {
                String stationId = node.getProperty(STATION_ID).toString();
                if (notJustSeenStation(stationId, results)) {
                    results.add(stationRepository.getStationById(stationId));
                }
            } else if (node.hasLabel(GraphBuilder.Labels.BUS_STATION)) {
                String stationId = node.getProperty(GraphStaticKeys.ID).toString();
                results.add(stationRepository.getStationById(stationId));
            } else if (node.hasLabel(GraphBuilder.Labels.QUERY_NODE)) {
                double lat = (double)node.getProperty(GraphStaticKeys.Walk.LAT);
                double lon = (double)node.getProperty(GraphStaticKeys.Walk.LONG);
                Location location = MyLocation.create(mapper, new LatLong(lat,lon));
                results.add(location);
            } else if (node.hasLabel(GraphBuilder.Labels.TRAM_STATION)) {
                String stationId = node.getProperty(GraphStaticKeys.ID).toString();
                if (notJustSeenStation(stationId, results)) {
                    results.add(stationRepository.getStationById(stationId));
                }
            }
        });
        return results;
    }

    private boolean notJustSeenStation(String stationId, List<Location> results) {
        if (results.isEmpty()) {
            return true;
        }
        Location previous = results.get(results.size()-1);
        return !previous.getId().equals(stationId);
    }

}
