package com.tramchester.graph.search;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tramchester.domain.IdFor;
import com.tramchester.domain.places.Location;
import com.tramchester.domain.places.MyLocation;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.graph.GraphPropertyKey;
import com.tramchester.graph.graphbuild.GraphBuilder;
import com.tramchester.repository.StationRepository;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;

import java.util.ArrayList;
import java.util.List;

public class MapPathToLocations {
    private final StationRepository stationRepository;
    private final ObjectMapper mapper;

    public MapPathToLocations(StationRepository stationRepository) {
        this.stationRepository = stationRepository;
        mapper = new ObjectMapper();
    }

    public List<Location> mapToLocations(Path path) {
        List<Location> results = new ArrayList<>();
        path.nodes().forEach(node -> mapNode(results, node));
        return results;
    }

    private void mapNode(List<Location> results, Node node) {
        if (node.hasLabel(GraphBuilder.Labels.ROUTE_STATION)) {
            IdFor<Station> stationId = IdFor.getStationIdFrom(node);
            if (notJustSeenStation(stationId, results)) {
                results.add(stationRepository.getStationById(stationId));
            }
        } else if (node.hasLabel(GraphBuilder.Labels.BUS_STATION)) {
            IdFor<Station> stationId = IdFor.getStationIdFrom(node);
            results.add(stationRepository.getStationById(stationId));
        } else if (node.hasLabel(GraphBuilder.Labels.QUERY_NODE)) {
            double lat = (double)node.getProperty(GraphPropertyKey.LATITUDE.getText());
            double lon = (double)node.getProperty(GraphPropertyKey.LONGITUDE.getText());
            Location location = MyLocation.create(mapper, new LatLong(lat,lon));
            results.add(location);
        } else if (node.hasLabel(GraphBuilder.Labels.TRAM_STATION)) {
            IdFor<Station> stationId = IdFor.getStationIdFrom(node);
            if (notJustSeenStation(stationId, results)) {
                results.add(stationRepository.getStationById(stationId));
            }
        }
    }

    private boolean notJustSeenStation(IdFor<Station> stationId, List<Location> results) {
        if (results.isEmpty()) {
            return true;
        }
        Location previous = results.get(results.size()-1);
        return !previous.forDTO().equals(stationId.forDTO());
    }

}
