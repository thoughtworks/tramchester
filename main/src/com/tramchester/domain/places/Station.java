package com.tramchester.domain.places;

import com.tramchester.domain.*;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.geo.GridPosition;
import com.tramchester.graph.GraphPropertyKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static java.lang.String.format;

public class Station extends MapIdToDTOId<Station> implements Location<Station> {
    private static final Logger logger = LoggerFactory.getLogger(Station.class);

    public static final String METROLINK_PREFIX = "9400ZZ";

    private final String area;
    private final IdFor<Station> id; // id is the "atoc code" for the tfgm data set
    private final String name;
    private final LatLong latLong;
    private final GridPosition gridPosition;
    private TransportMode transportMode;
    private final List<Platform> platforms;
    private final Set<Route> servesRoutes;
    private final Set<Agency> servesAgencies;

    public Station(IdFor<Station> id, String area, String stationName, LatLong latLong, GridPosition gridPosition) {
        this.gridPosition = gridPosition;
        platforms = new LinkedList<>();
        servesRoutes = new HashSet<>();
        servesAgencies = new HashSet<>();

        this.id = id;
        this.name = stationName;
        this.transportMode = TransportMode.NotSet; // can't determine reliably until know the route
        this.latLong = latLong;
        this.area = area;
    }

    @Override
    public IdFor<Station> getId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public LatLong getLatLong() {
        return latLong;
    }

    // Metrolink stations ID the platform with a number on the end of the stationID
    public static IdFor<Station> formId(String text) {
        if (text.startsWith(METROLINK_PREFIX)) {
            // metrolink platform ids include platform as final digit, remove to give id of station itself
            int index = text.length()-1;
            return IdFor.createId(text.substring(0,index));
        }
        return IdFor.createId(text);
    }

    @Override
    public String getArea() {
        return area;
    }

    @Override
    public boolean hasPlatforms() {
        return !platforms.isEmpty();
    }

    @Override
    public List<Platform> getPlatforms() {
        return platforms;
    }

    @Override
    public TransportMode getTransportMode() {
        return transportMode;
    }

    public List<Platform> getPlatformsForRoute(Route route) {
        return platforms.stream().filter(platform -> platform.getRoutes().contains(route)).collect(Collectors.toList());
    }

    public void addPlatform(Platform platform) {
        if (!platforms.contains(platform)) {
            platforms.add(platform);
        }
    }

    public boolean hasPlatform(IdFor<Platform> platformId) {
        return platforms.stream().map(Platform::getId).anyMatch(id -> id.equals(platformId));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;

        if (!(o instanceof Station)) return false;

        Station station = (Station) o;

        return id.equals(station.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "Station{" +
                "area='" + area + '\'' +
                ", id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", latLong=" + latLong +
                ", mode=" + transportMode +
                ", platforms=" + HasId.asIds(platforms) +
                ", servesRoutes=" + HasId.asIds(servesRoutes) +
                '}';
    }

    public void addRoute(Route route) {
        servesRoutes.add(route);
        if (transportMode.equals(TransportMode.NotSet)) {
            transportMode = route.getTransportMode();
        } else if (!transportMode.equals(route.getTransportMode())) {
            String message = format("Here to detect if multi-mode stations exist, route %s and station %s",
                    route, this.toString());
            logger.error(message);
            throw new RuntimeException(message);
        }

        servesAgencies.add(route.getAgency());
    }

    public Set<Route> getRoutes() {
        return servesRoutes;
    }

    public Set<Agency> getAgencies() {
        return servesAgencies;
    }

    public boolean servesRoute(Route route) {
        return servesRoutes.contains(route);
    }

    public GridPosition getGridPosition() {
        return gridPosition;
    }

    @Override
    public GraphPropertyKey getProp() {
        return GraphPropertyKey.STATION_ID;
    }
}
