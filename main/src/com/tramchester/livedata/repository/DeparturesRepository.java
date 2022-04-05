package com.tramchester.livedata.repository;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.Platform;
import com.tramchester.domain.places.Location;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.places.StationGroup;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TramTime;
import com.tramchester.geo.MarginInMeters;
import com.tramchester.geo.StationLocationsRepository;
import com.tramchester.livedata.domain.liveUpdates.UpcomingDeparture;
import com.tramchester.livedata.tfgm.TramDepartureRepository;
import com.tramchester.repository.StationGroupsRepository;
import org.apache.commons.collections4.SetUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.String.format;

@LazySingleton
public class DeparturesRepository {
    private static final Logger logger = LoggerFactory.getLogger(DeparturesRepository.class);

    private final StationLocationsRepository stationLocationsRepository;
    private final TramDepartureRepository tramDepartureRepository;
    private final TramchesterConfig config;

    @Inject
    public DeparturesRepository(StationLocationsRepository stationLocationsRepository,
                                TramDepartureRepository tramDepartureRepository, TramchesterConfig config,
                                StationGroupsRepository stationGroupsRepository) {
        this.stationLocationsRepository = stationLocationsRepository;
        this.tramDepartureRepository = tramDepartureRepository;
        this.config = config;
    }

    public List<UpcomingDeparture> dueTramsForLocation(Location<?> location, LocalDate date, TramTime time,
                                                       Set<TransportMode> modes) {
        List<UpcomingDeparture> departures = switch (location.getLocationType()) {
            case Station -> getStationDepartures((Station) location, date, time, modes);
            case StationGroup -> getStationGroupDepartures((StationGroup) location, date, time, modes);
            case MyLocation, Postcode -> getDeparturesNearTo(location, date, time, modes);
            case Platform -> getPlatformDepartrues((Platform) location, date, time, modes);
        };
        return departures.stream().filter(departure -> departure.getDate().equals(date)).collect(Collectors.toList());
    }

    private List<UpcomingDeparture> getPlatformDepartrues(Platform platform, LocalDate date, TramTime time, Set<TransportMode> modes) {
        if (!TransportMode.intersects(modes, platform.getTransportModes())) {
            logger.error(format("Platform %s does not match supplied modes %s", platform, modes));
        }
        return tramDepartureRepository.dueTramsForPlatform(platform.getId(), date, time);
    }

    private List<UpcomingDeparture> getDeparturesNearTo(Location<?> location, LocalDate date, TramTime time, Set<TransportMode> modes) {
        final MarginInMeters margin = MarginInMeters.of(config.getNearestStopRangeKM());
        final int numOfNearestStopsToOffer = config.getNumOfNearestStopsToOffer();

        List<Station> nearbyStations = stationLocationsRepository.nearestStationsSorted(location, numOfNearestStopsToOffer,
                margin, modes);

        return nearbyStations.stream().
                flatMap(station -> getStationDepartures(station, date, time, modes).stream()).
                distinct().
                collect(Collectors.toList());
    }

    private List<UpcomingDeparture> getStationGroupDepartures(StationGroup stationGroup, LocalDate date, TramTime time, Set<TransportMode> modes) {
        return stationGroup.getContained().stream().
                filter(station -> TransportMode.intersects(station.getTransportModes(), modes)).
                flatMap(station -> getStationDepartures(station, date, time, modes).stream()).
                distinct().collect(Collectors.toList());

    }

    private List<UpcomingDeparture> getStationDepartures(Station station, LocalDate date, TramTime time, Set<TransportMode> modes) {
        SetUtils.SetView<TransportMode> toFetch = SetUtils.intersection(station.getTransportModes(), modes);
        if (toFetch.isEmpty()) {
            logger.error(format("Station modes %s and filter modes %s do not overlap", station, modes));
        }

        return toFetch.stream().
                flatMap(mode -> getDeparturesFor(mode, station, date, time)).
                collect(Collectors.toList());
    }

    private Stream<UpcomingDeparture> getDeparturesFor(TransportMode mode, Station station, LocalDate date, TramTime time) {
        return switch (mode) {
            case Tram -> tramDepartureRepository.dueTramsForStation(station, date, time).stream();
            default -> {
                final String msg = "TODO - live data for " + mode + " is not implemented yet";
                logger.info(msg);
                throw new RuntimeException(msg); }

        };
    }
}
