package com.tramchester.domain.places;

import com.tramchester.domain.Route;
import com.tramchester.domain.StationLink;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.reference.TransportMode;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static java.lang.String.format;

public class LinkedInterchangeStation implements InterchangeStation {
    private final Set<StationLink> links;
    private final Station origin;
    private final EnumSet<TransportMode> allModes;

    public LinkedInterchangeStation(StationLink stationLink) {
        links = new HashSet<>();
        links.add(stationLink);
        origin = stationLink.getBegin();
        Set<TransportMode> collectedModes = links.stream().flatMap(links -> links.getContainedModes().stream()).collect(Collectors.toSet());
        this.allModes = EnumSet.copyOf(collectedModes);
    }

    @Override
    public boolean isMultiMode() {
        return allModes.size() > 1;
    }

    @Override
    public Set<Route> getDropoffRoutes() {
        return origin.getDropoffRoutes();
    }

    @Override
    public Set<Route> getPickupRoutes() {
        Set<Route> pickUps = new HashSet<>(origin.getPickupRoutes());
        Set<Route> otherEnd = links.stream().map(StationLink::getEnd).
                flatMap(station -> station.getPickupRoutes().stream()).collect(Collectors.toSet());
        pickUps.addAll(otherEnd);
        return pickUps;
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

    @Override
    public EnumSet<TransportMode> getTransportModes() {
        return allModes;
    }

    public void addLink(StationLink stationLink) {
        if (!stationLink.getBegin().equals(origin)) {
            throw new RuntimeException(format("Attempt to add a stationlink (%s) that does not match origin %s", stationLink, origin));
        }
        if (links.contains(stationLink)) {
            throw new RuntimeException(format("Attempt to add duplicated link %s to %s", stationLink, links));
        }
        links.add(stationLink);
    }

    @Override
    public String toString() {
        return "LinkedInterchangeStation{" +
                "links=" + links +
                ", origin=" + origin.getId() +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LinkedInterchangeStation that = (LinkedInterchangeStation) o;
        return links.equals(that.links) && origin.equals(that.origin);
    }

    @Override
    public int hashCode() {
        return Objects.hash(links, origin);
    }

    public Set<Station> getLinked() {
        return links.stream().map(StationLink::getEnd).collect(Collectors.toSet());
    }

    @Override
    public IdFor<Station> getId() {
        return getStationId();
    }
}
