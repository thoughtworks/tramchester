package com.tramchester.domain.places;

import com.tramchester.domain.Route;
import com.tramchester.domain.StationLink;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.reference.TransportMode;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static java.lang.String.format;

public class MultiInterchangeStation implements InterchangeStation {
    private final Set<StationLink> links;
    private final Station origin;

    public MultiInterchangeStation(StationLink stationLink) {
        links = new HashSet<>();
        links.add(stationLink);
        origin = stationLink.getBegin();
    }

    @Override
    public boolean isMultiMode() {
        Set<TransportMode> allModes = links.stream().flatMap(links -> links.getContainedModes().stream()).collect(Collectors.toSet());
        return allModes.size() > 1;
    }

    @Override
    public Set<Route> getDropoffRoutes() {
        return null;
    }

    @Override
    public Set<Route> getPickupRoutes() {
        return null;
    }

    @Override
    public IdFor<Station> getStationId() {
        return origin.getId();
    }

    @Override
    public InterchangeType getType() {
        return InterchangeType.NeighbourLinks;
    }

    @Override
    public Station getStation() {
        return origin;
    }

    public void addLink(StationLink stationLink) {
        if (!stationLink.getBegin().equals(origin)) {
            String msg = format("Attempt to add a stationlink (%s) that does not match origin %s", stationLink, origin);
            throw new RuntimeException(msg);
        }
        links.add(stationLink);
    }


}
