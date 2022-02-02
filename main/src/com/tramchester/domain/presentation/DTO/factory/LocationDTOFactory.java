package com.tramchester.domain.presentation.DTO.factory;

import com.tramchester.domain.Route;
import com.tramchester.domain.places.Location;
import com.tramchester.domain.presentation.DTO.LocationDTO;
import com.tramchester.domain.presentation.DTO.PlatformDTO;
import com.tramchester.domain.presentation.DTO.RouteRefDTO;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class LocationDTOFactory {


    public LocationDTO createLocationDTO(Location<?> source) {
        final List<PlatformDTO> platforms = createPlatformDTOs(source);
        final List<RouteRefDTO> routes = createRouteDTOs(source);
        return new LocationDTO(source, platforms, routes);
    }

    @NotNull
    private List<RouteRefDTO> createRouteDTOs(Location<?> source) {
        final Stream<Route> routeStream = Stream.concat(source.getDropoffRoutes().stream(), source.getPickupRoutes().stream());
        return routeStream.map(RouteRefDTO::new).collect(Collectors.toList());
    }

    @NotNull
    private List<PlatformDTO> createPlatformDTOs(Location<?> source) {
        if (source.hasPlatforms()) {
            return source.getPlatforms().stream().map(PlatformDTO::new).collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}
